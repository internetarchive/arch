package org.archive.webservices.ars.io

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.collections.inputspecs.{ArchCollectionSpecLoader, FilePointer, FileRecord, InputSpec}
import org.archive.webservices.ars.model.collections.{CollectionSpecifics, GenericRandomAccess}
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.cdx.{CdxRecord, CdxUtil}
import org.archive.webservices.sparkling.http.HttpClient
import org.archive.webservices.sparkling.io.{ChainedInputStream, HdfsIO, IOUtil}
import org.archive.webservices.sparkling.util.{CleanupIterator, IteratorUtil, RddUtil}
import org.archive.webservices.sparkling.warc.{WarcLoader, WarcRecord}

import java.io.{BufferedInputStream, InputStream}
import java.net.URL
import scala.collection.mutable
import scala.util.Try

object WebArchiveLoader {
  val WasapiPageSize = 100
  val WarcFilesPerPartition = 5
  val RetrySleepMillis = 5000
  val CdxSkipDistance: Long = 10.mb

  def loadCdx[R](spec: InputSpec)(action: RDD[CdxRecord] => R): R =
    loadCdxFromWarcFiles(spec)(action)

  def loadCdxFromWarcFiles[R](spec: InputSpec)(action: RDD[CdxRecord] => R): R = {
    loadWarcFiles(spec)(loadCdxFromWarcFileRecords(_)(action))
  }

  def loadCdxFromWarcFileRecords[R](rdd: RDD[FileRecord])(action: RDD[CdxRecord] => R): R = {
    action(rdd.flatMap { record =>
      CdxUtil.fromWarcGzStream(record.filename, record.access).map { r =>
        val Seq(offsetStr, _) = r.additionalFields
        r.copy(additionalFields = Seq(offsetStr, record.pointer.url))
      }
    })
  }

  def loadCdxFromWarcGzStreams(rdd: RDD[(FilePointer, InputStream)]): RDD[CdxRecord] = {
    rdd.flatMap { case (file, in) =>
      CdxUtil.fromWarcGzStream(file.filename, in).map { r =>
        val Seq(offsetStr, _) = r.additionalFields
        r.copy(additionalFields = Seq(offsetStr, file.url))
      }
    }
  }

  def loadWarcFiles[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.loader.load(spec) { rdd =>
      action(rdd.filter(_.mime == ArchCollectionSpecLoader.WarcMime))
    }
  }

  def loadWarcsRecords[R](inputSpec: InputSpec)(
      action: RDD[(FilePointer, CleanupIterator[WarcRecord])] => R): R = {
    loadWarcFiles(inputSpec) { rdd =>
      action(rdd.map { file =>
        val in = file.access
        val warcs = WarcLoader.load(in).filter(r => r.isResponse || r.isRevisit)
        (
          file.pointer,
          IteratorUtil.cleanup(
            IteratorUtil.whileDefined {
              Try {
                if (warcs.hasNext) Try(warcs.next).toOption else None
              }.toOption.flatten
            },
            in.close))
      })
    }
  }

  def loadWarcs[R](spec: InputSpec)(action: RDD[WarcRecord] => R): R = {
    loadWarcsRecords(spec)(rdd => action(rdd.mapPartitions(_.flatMap(_._2))))
  }

  def loadWarcFiles[R](inputPath: String, hdfsHostPort: Option[(String, Int)] = None)(
      action: RDD[(String, InputStream)] => R): R = action {
    (hdfsHostPort match {
      case Some((host, port)) =>
        val files = HdfsIO(host, port).files(inputPath + "/*arc.gz", recursive = false).toSeq
        RddUtil
          .parallelize(files.grouped(WarcFilesPerPartition).toSeq)
          .mapPartitions { paths =>
            val hdfsIO = HdfsIO(host, port)
            paths.flatten.map(p => (p, hdfsIO.open(p, decompress = false)))
          }
      case None =>
        val numFiles = HdfsIO.files(inputPath + "/*arc.gz", recursive = false).size
        RddUtil
          .loadFilesLocality(inputPath + "/*arc.gz")
          .coalesce(numFiles / WarcFilesPerPartition + 1)
          .map(p => (p, HdfsIO.open(p, decompress = false)))
    }).mapPartitions { partition =>
      var prev: Option[InputStream] = None
      partition.map { case (p, in) =>
        for (s <- prev) s.close()
        prev = Some(in)
        (p.split('/').last, prev.get)
      } ++ IteratorUtil.noop {
        for (s <- prev) s.close()
      }
    }
  }

  def loadAitWarcFiles[R](aitId: Int, inputPath: String, cacheId: String)(
      action: RDD[(String, InputStream)] => R): R = {
    val basicAuth = ArchConf.foreignAitAuthHeader
    val hdfsHostPort = ArchConf.aitCollectionHdfsHostPort
    if (basicAuth.isDefined) {
      val wasapiUrl =
        ArchConf.aitWarcsBaseUrl + "/wasapi/v1/webdata?format=json&filetype=warc&collection=" + aitId + "&page_size="
      var apiFileCount = -1
      while (apiFileCount < 0) {
        Ait
          .getJsonWithAuth(wasapiUrl + 1, basicAuth = basicAuth) { json =>
            json.get[Int]("count").toOption
          } match {
          case Right(i) => apiFileCount = i
          case Left(status) =>
            if (status / 100 != 5) Thread.sleep(RetrySleepMillis) else apiFileCount = 0
        }
      }
      val hdfsFileCount = hdfsHostPort
        .map { case (host, port) =>
          HdfsIO(host, port).files(inputPath + "/*arc.gz", recursive = false).size
        }
        .getOrElse(0)
      if (hdfsFileCount == apiFileCount) {
        loadWarcFiles(inputPath, hdfsHostPort)(action)
      } else {
        CollectionCache.cache(cacheId) { cachePath =>
          val cacheFileCount = HdfsIO.files(cachePath + "/*arc.gz", recursive = false).size
          if (hdfsFileCount + cacheFileCount == apiFileCount) {
            if (hdfsHostPort.isDefined) {
              loadWarcFiles(inputPath, hdfsHostPort) { rdd =>
                loadWarcFiles(cachePath) { cachedRdd =>
                  action(rdd.union(cachedRdd))
                }
              }
            } else loadWarcFiles(cachePath)(action)
          } else
            action {
              RddUtil
                .parallelize((apiFileCount.toDouble / WasapiPageSize).ceil.toInt)
                .flatMap { idx =>
                  var wasapiOpt: Option[Iterable[(String, String)]] = None
                  while (wasapiOpt.isEmpty) {
                    Ait
                      .getJsonWithAuth(
                        wasapiUrl + WasapiPageSize + "&page=" + (idx + 1),
                        basicAuth = basicAuth) { json =>
                        json
                          .downField("files")
                          .values
                          .map(_.flatMap { fileJson =>
                            val fileCursor = fileJson.hcursor
                            for {
                              filename <- fileCursor.get[String]("filename").toOption
                              location <- fileCursor
                                .downField("locations")
                                .values
                                .flatMap(_.flatMap(_.asString).find(
                                  _.startsWith(ArchConf.aitWarcsBaseUrl)))
                            } yield (filename, location)
                          })
                      } match {
                      case Right(iter) => wasapiOpt = Some(iter)
                      case Left(status) =>
                        if (status / 100 == 5) Thread.sleep(RetrySleepMillis)
                        else wasapiOpt = Some(Iterable.empty[(String, String)])
                    }
                  }
                  wasapiOpt.get
                }
                .repartition(apiFileCount / WarcFilesPerPartition + 1)
                .mapPartitions { partition =>
                  var prev: Option[InputStream] = None
                  val aitHdfsIO = hdfsHostPort.map { case (host, port) => HdfsIO(host, port) }
                  partition
                    .flatMap { case (file, location) =>
                      val inputFilePath = inputPath + "/" + file
                      if (aitHdfsIO.exists(_.exists(inputFilePath))) {
                        Some((inputFilePath, inputFilePath, true))
                      } else {
                        val cacheFilePath = cachePath + "/" + file
                        if (HdfsIO.exists(cacheFilePath)) {
                          Some((inputFilePath, cacheFilePath, false))
                        } else {
                          IOHelper.syncHdfs(cacheFilePath + "_caching") {
                            if (HdfsIO.exists(cacheFilePath))
                              Some((inputFilePath, cacheFilePath, false))
                            else {
                              Ait
                                .getWithAuth(
                                  location,
                                  contentType = "*/*",
                                  basicAuth = basicAuth) { in =>
                                  val out =
                                    HdfsIO.out(
                                      cacheFilePath,
                                      compress = false,
                                      overwrite = true,
                                      useWriter = false)
                                  try {
                                    IOUtil.copy(in, out)
                                  } finally {
                                    out.close()
                                  }
                                  while (!HdfsIO.exists(cacheFilePath)) Thread.`yield`()
                                  Some((inputFilePath, cacheFilePath, false))
                                }
                                .toOption
                            }
                          }
                        }
                      }
                    }
                    .map { case (originalPath, path, ait) =>
                      for (s <- prev) s.close()
                      val in = (if (ait) aitHdfsIO.get else HdfsIO).open(path, decompress = false)
                      prev = Some(in)
                      (originalPath, in)
                    } ++ IteratorUtil.noop {
                    for (s <- prev) s.close()
                  }
                }
            }
        }
      }
    } else {
      loadWarcFiles(inputPath, hdfsHostPort)(action)
    }
  }

  private def loadWarcFilesViaCdx(cdxPath: String)(
      in: Iterator[((FilePointer, Long), Iterator[(CdxRecord, Long, Long)])] => Iterator[
        InputStream]): RDD[(String, InputStream)] = {
    val inputPath = s"$cdxPath/*.cdx.gz"
    val numFiles = HdfsIO.files(inputPath, recursive = false).size
    RddUtil
      .loadTextFiles(inputPath)
      .map { case (file, lines) =>
        val pointers = lines.flatMap(CdxRecord.fromString).flatMap { cdx =>
          Try {
            val length = cdx.compressedSize
            val (path, offset) = cdx.locationFromAdditionalFields
            (cdx, path, offset, length)
          }.toOption
        }
        var prevGroup: Option[(String, Long, (String, Long))] = None
        val groups = IteratorUtil
          .groupSortedBy(pointers) { case (_, path, offset, _) =>
            val group = prevGroup
              .filter { case (p, o, _) =>
                p == path && offset > o && offset <= o + CdxSkipDistance
              }
              .map(_._3)
              .getOrElse {
                (path, offset)
              }
            prevGroup = Some((path, offset, group))
            group
          }
          .map { case ((file, initialOffset), group) =>
            (
              (FilePointer.fromUrl(file), initialOffset),
              group.map { case (r, _, o, l) => (r, o, l) })
          }
        (
          file.split('/').last,
          new BufferedInputStream(new ChainedInputStream(in(groups))).asInstanceOf[InputStream])
      }
      .coalesce(numFiles / WarcFilesPerPartition + 1)
  }

  def loadWarcFilesViaCdxFiles(cdxPath: String): RDD[(String, InputStream)] = {
    val accessContext = CollectionAccessContext.fromLocalArchConf
    loadWarcFilesViaCdx(cdxPath) { partition =>
      accessContext.init()
      val collectionSpecificsCache = mutable.Map.empty[String, Option[CollectionSpecifics]]
      partition.map { case ((pointer, initialOffset), positions) =>
        val offsetPositions = positions.map { case (_, offset, length) =>
          (offset - initialOffset, length)
        }
        if (pointer.isHttpSource) {
          val in = new URL(pointer.url).openStream
          IOUtil.skip(in, initialOffset)
          IOHelper.splitMergeInputStreams(in, offsetPositions, buffered = false)
        } else if (pointer.isCollectionSource) {
          GenericRandomAccess.randomAccess(
            accessContext,
            pointer,
            initialOffset,
            offsetPositions,
            collectionSpecificsCache)
        } else throw new UnsupportedOperationException()
      }
    }
  }

  def loadWarcFilesViaCdxFromCollections(
      cdxPath: String,
      collectionId: String): RDD[(String, InputStream)] = {
    val accessContext = CollectionAccessContext.fromLocalArchConf
    loadWarcFilesViaCdx(cdxPath) { partition =>
      accessContext.init()
      CollectionSpecifics.get(collectionId).toIterator.flatMap { specifics =>
        partition.map { case ((pointer, initialOffset), positions) =>
          specifics.randomAccess(
            accessContext,
            specifics.inputPath,
            pointer,
            initialOffset,
            positions.map { case (_, offset, length) =>
              (offset - initialOffset, length)
            })
        }
      }
    }
  }

  def randomAccessPetabox(
      context: CollectionAccessContext,
      itemFilePath: String,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    val url = ArchConf.iaBaseUrl + "/serve/" + itemFilePath
    HttpClient.rangeRequest(
      url,
      headers = ArchConf.foreignAitAuthHeader.map("Authorization" -> _).toMap,
      offset = offset) { in =>
      IOHelper.splitMergeInputStreams(in, positions)
    }
  }

  def loadWarcFilesViaCdxFromPetabox(cdxPath: String): RDD[(String, InputStream)] = {
    val accessContext = CollectionAccessContext.fromLocalArchConf
    loadWarcFilesViaCdx(cdxPath) { partition =>
      accessContext.init()
      partition.map { case ((pointer, initialOffset), positions) =>
        randomAccessPetabox(
          accessContext,
          pointer.filename,
          initialOffset,
          positions.map { case (_, offset, length) =>
            (offset - initialOffset, length)
          })
      }
    }
  }

  def randomAccessHdfs(
      context: CollectionAccessContext,
      filePath: String,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    val in = context.hdfsIO.open(
      filePath,
      offset = offset,
      decompress = false,
      strategy = HdfsIO.LoadingStrategy.Remote)
    IOHelper.splitMergeInputStreams(in, positions)
  }

  def loadWarcFilesViaCdxFromHdfs(
      cdxPath: String,
      warcPath: String,
      aitHdfs: Boolean = false): RDD[(String, InputStream)] = {
    val accessContext = CollectionAccessContext.fromLocalArchConf(alwaysAitHdfsIO = aitHdfs)
    loadWarcFilesViaCdx(cdxPath) { partition =>
      accessContext.init()
      partition.map { case ((pointer, initialOffset), positions) =>
        randomAccessHdfs(
          accessContext,
          warcPath + "/" + pointer.filename,
          initialOffset,
          positions.map { case (_, offset, length) =>
            (offset - initialOffset, length)
          })
      }
    }
  }

  def randomAccessAit(
      context: CollectionAccessContext,
      sourceId: String,
      filePath: String,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    if (context.aitHdfsIO.exists(_.exists(filePath))) {
      val in = context.aitHdfsIO.get.open(
        filePath,
        offset = offset,
        decompress = false,
        strategy = HdfsIO.LoadingStrategy.Remote)
      IOHelper.splitMergeInputStreams(in, positions)
    } else {
      val file = filePath.split('/').last
      val cachePath = CollectionCache.cachePath(sourceId, file)
      if (HdfsIO.exists(cachePath)) {
        val in = HdfsIO.open(
          cachePath,
          offset = offset,
          decompress = false,
          strategy = HdfsIO.LoadingStrategy.Remote)
        IOHelper.splitMergeInputStreams(in, positions)
      } else {
        HttpClient.rangeRequest(
          ArchConf.aitWarcsBaseUrl + "/webdatafile/" + file,
          headers = ArchConf.foreignAitAuthHeader.map("Authorization" -> _).toMap,
          offset = offset) { in =>
          IOHelper.splitMergeInputStreams(in, positions)
        }
      }
    }
  }
}

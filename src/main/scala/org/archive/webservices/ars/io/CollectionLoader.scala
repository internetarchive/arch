package org.archive.webservices.ars.io

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.collections.CollectionSpecifics
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.http.HttpClient
import org.archive.webservices.sparkling.io.{ChainedInputStream, HdfsIO, IOUtil}
import org.archive.webservices.sparkling.util.{CleanupIterator, IteratorUtil, RddUtil, StringUtil}
import org.archive.webservices.sparkling.warc.{WarcLoader, WarcRecord}

import java.io.{BufferedInputStream, InputStream}
import scala.util.Try

object CollectionLoader {
  val WasapiPageSize = 100
  val WarcFilesPerPartition = 5
  val RetrySleepMillis = 5000
  val CdxSkipDistance: Long = 10.mb

  def loadWarcFiles[R](collectionId: String, inputPath: String)(action: RDD[(CollectionSourcePointer, InputStream)] => R): R = {
    CollectionSpecifics.get(collectionId) match {
      case Some(specifics) =>
        specifics.loadWarcFiles(inputPath)(rdd => action(rdd.map { case (file, in) =>
          (CollectionSourcePointer(specifics.sourceId, file), in)
        }))
      case None => loadWarcFiles(inputPath)(rdd => action(rdd.map { case (file, in) =>
        (CollectionSourcePointer(collectionId, file), in)
      }))
    }
  }

  def loadWarcsWithSource[R](
    collectionId: String,
    inputPath: String)(action: RDD[(CollectionSourcePointer, CleanupIterator[WarcRecord])] => R): R = {
    loadWarcFiles(collectionId, inputPath)(rdd => action(rdd.map {
      case (p, in) =>
        val warcs = WarcLoader
          .load(in)
          .filter(r => r.isResponse || r.isRevisit)
        (
          p,
          IteratorUtil.cleanup(
            IteratorUtil.whileDefined {
              Try {
                if (warcs.hasNext) Try(warcs.next).toOption else None
              }.toOption.flatten
            },
            in.close))
    }))
  }

  def loadWarcs[R](id: String, inputPath: String)(action: RDD[WarcRecord] => R): R =
    loadWarcsWithSource(id, inputPath)(rdd => action(rdd.mapPartitions(_.flatMap(_._2))))

  def loadWarcFiles[R](
      inputPath: String,
      hdfsHostPort: Option[(String, Int)] = None)(action: RDD[(String, InputStream)] => R): R = action {
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
      partition.map {
        case (p, in) =>
          for (s <- prev) s.close()
          prev = Some(in)
          (p, prev.get)
      } ++ IteratorUtil.noop {
        for (s <- prev) s.close()
      }
    }
  }

  def loadAitWarcFiles[R](
      aitId: Int,
      inputPath: String,
      cacheId: String)(action: RDD[(String, InputStream)] => R): R = {
    val basicAuth = ArchConf.foreignAitAuthHeader
    val hdfsHostPort = ArchConf.aitCollectionHdfsHostPort
    if (basicAuth.isDefined) {
      val wasapiUrl = "https://warcs.archive-it.org/wasapi/v1/webdata?format=json&filetype=warc&collection=" + aitId + "&page_size="
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
        .map { case (host, port) => HdfsIO(host, port) }
        .getOrElse(HdfsIO)
        .files(inputPath + "/*arc.gz", recursive = false)
        .size
      if (hdfsFileCount == apiFileCount) {
        loadWarcFiles(inputPath, hdfsHostPort)(action)
      } else {
        CollectionCache.cache(cacheId) { cachePath =>
          val cacheFileCount = HdfsIO.files(cachePath + "/*arc.gz", recursive = false).size
          if (hdfsFileCount + cacheFileCount == apiFileCount) {
            loadWarcFiles(inputPath, hdfsHostPort) { rdd =>
              loadWarcFiles(cachePath) { cachedRdd =>
                action(rdd.union(cachedRdd))
              }
            }
          } else action {
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
                                _.startsWith("https://warcs.archive-it.org")))
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
                val hdfsIO = hdfsHostPort
                  .map { case (host, port) => HdfsIO(host, port) }
                  .getOrElse(HdfsIO)
                partition
                  .flatMap {
                    case (file, location) =>
                      val inputFilePath = inputPath + "/" + file
                      if (hdfsIO.exists(inputFilePath)) Some((inputFilePath, inputFilePath, true))
                      else {
                        val cacheFilePath = cachePath + "/" + file
                        if (HdfsIO.exists(cacheFilePath))
                          Some((inputFilePath, cacheFilePath, false))
                        else {
                          IOHelper.syncHdfs(cacheFilePath + "_caching") {
                            if (HdfsIO.exists(cacheFilePath))
                              Some((inputFilePath, cacheFilePath, false))
                            else {
                              Ait
                                .getWithAuth(location, contentType = "*/*", basicAuth = basicAuth) {
                                  in =>
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
                  .map {
                    case (originalPath, path, ait) =>
                      for (s <- prev) s.close()
                      val in = (if (ait) hdfsIO else HdfsIO).open(path, decompress = false)
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
      action: Iterator[((CollectionSourcePointer, Long), Iterator[(CdxRecord, Long, Long)])] => Iterator[
        InputStream]): RDD[(String, InputStream)] = {
    val inputPath = s"$cdxPath/*.cdx.gz"
    val numFiles = HdfsIO.files(inputPath, recursive = false).size
    RddUtil
      .loadTextFiles(inputPath)
      .mapPartitions { partition =>
        partition.map {
          case (file, lines) =>
            val pointers = lines.flatMap(CdxRecord.fromString).map { cdx =>
              val length = cdx.compressedSize
              val (path, offset) = cdx.locationFromAdditionalFields
              (cdx, path, offset, length)
            }
            var prevGroup: Option[(String, Long, (String, Long))] = None
            val groups = IteratorUtil.groupSortedBy(pointers) {
              case (_, path, offset, _) =>
                val group = prevGroup
                  .filter {
                    case (p, o, _) => p == path && offset > o && offset <= o + CdxSkipDistance
                  }
                  .map(_._3)
                  .getOrElse {
                    (path, offset)
                  }
                prevGroup = Some((path, offset, group))
                group
            }.map { case ((file, initialOffset), group) =>
              ((CollectionSourcePointer(
                StringUtil.prefixBySeparator(file, UserDefinedQuery.CollectionLocationSeparator),
                StringUtil.stripPrefixBySeparator(file, UserDefinedQuery.CollectionLocationSeparator)), initialOffset),
                group.map { case (r, _, o, l) => (r, o, l) })
            }
            val in = action(groups)
            (
              file.split('/').last,
              new BufferedInputStream(new ChainedInputStream(in, nextOnError = true))
                .asInstanceOf[InputStream])
        }
      }
      .coalesce(numFiles / WarcFilesPerPartition + 1)
  }

  def loadWarcFilesViaCdxFromCollections(cdxPath: String, collectionId: String): RDD[(String, InputStream)] = {
    val accessContext = CollectionAccessContext.fromArchConf
    loadWarcFilesViaCdx(cdxPath) { partition =>
      CollectionSpecifics.get(collectionId).toIterator.flatMap { specifics =>
        partition.flatMap { case ((pointer, initialOffset), positions) =>
          specifics.randomAccess(accessContext, specifics.inputPath, pointer, initialOffset, positions.map { case (_, offset, length) =>
            (offset, length)
          })
        }
      }
    }
  }

  def randomAccessPetabox(context: CollectionAccessContext, itemFilePath: String, initialOffset: Long, positions: Iterator[(Long, Long)]): Iterator[InputStream] = {
    val url = "https://archive.org/serve/" + itemFilePath
    val in = HttpClient.rangeRequest(
      url,
      headers = context.iaAuthHeader.map("Authorization" -> _).toMap,
      offset = initialOffset,
      close = false)(identity)
    IOUtil.splitStream(in, positions.map {
      case (offset, length) => (offset - initialOffset, length)
    })
  }

  def loadWarcFilesViaCdxFromPetabox(
      cdxPath: String): RDD[(String, InputStream)] = {
    val accessContext = CollectionAccessContext.fromArchConf
    loadWarcFilesViaCdx(cdxPath) { partition =>
      partition.flatMap {
        case ((pointer, initialOffset), positions) =>
          randomAccessPetabox(accessContext, pointer.filename, initialOffset, positions.map { case (_, offset, length) =>
            (offset, length)
          })
      }
    }
  }

  def randomAccessHdfs(context: CollectionAccessContext, filePath: String, initialOffset: Long, positions: Iterator[(Long, Long)]): Iterator[InputStream] = {
    val in = context.hdfsIO.open(filePath, offset = initialOffset, decompress = false)
    IOUtil.splitStream(in, positions.map {
      case (offset, length) => (offset - initialOffset, length)
    })
  }

  def loadWarcFilesViaCdxFromHdfs(
      cdxPath: String,
      warcPath: String,
      aitHdfs: Boolean = false): RDD[(String, InputStream)] = {
    val accessContext = CollectionAccessContext.fromArchConf(alwaysAitHdfsIO = aitHdfs)
    loadWarcFilesViaCdx(cdxPath) { partition =>
      partition.flatMap {
        case ((pointer, initialOffset), positions) =>
          randomAccessHdfs(accessContext, warcPath + "/" + pointer.filename, initialOffset, positions.map { case (_, offset, length) =>
            (offset, length)
          })
      }
    }
  }

  def randomAccessAit(context: CollectionAccessContext, sourceId: String, filePath: String, initialOffset: Long, positions: Iterator[(Long, Long)]): Iterator[InputStream] = {
    val in =
      if (context.aitHdfsIO.exists(filePath)) context.aitHdfsIO.open(filePath, offset = initialOffset, decompress = false)
      else {
        val file = filePath.split('/').last
        val p = context.cachePath(sourceId, file)
        if (HdfsIO.exists(p)) HdfsIO.open(p, offset = initialOffset, decompress = false)
        else {
          val url = "https://warcs.archive-it.org/webdatafile/" + file
          HttpClient.rangeRequest(
            url,
            headers = context.aitAuth.map("Authorization" -> _).toMap,
            offset = initialOffset,
            close = false)(identity)
        }
      }
    IOUtil.splitStream(in, positions.map {
      case (offset, length) => (offset - initialOffset, length)
    })
  }
}

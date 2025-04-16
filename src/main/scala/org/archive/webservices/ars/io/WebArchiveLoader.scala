package org.archive.webservices.ars.io

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.collections.CollectionSpecifics
import org.archive.webservices.ars.model.collections.inputspecs.{FileRecord, InputSpec, InputSpecLoader}
import org.archive.webservices.ars.util.Common
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.cdx.{CdxRecord, CdxUtil}
import org.archive.webservices.sparkling.http.HttpClient
import org.archive.webservices.sparkling.io.{ChainedInputStream, HdfsIO, IOUtil}
import org.archive.webservices.sparkling.util.{CleanupIterator, IteratorUtil, RddUtil}
import org.archive.webservices.sparkling.warc.{WarcLoader, WarcRecord}

import java.io.{BufferedInputStream, InputStream}
import scala.util.Try

object WebArchiveLoader {
  val WasapiPageSize = 100
  val WarcFilesPerPartition = 5
  val RetrySleepMillis = 5000
  val WasapiAttempts = 10
  val CdxSkipDistance: Long = 10.mb
  val WarcMime = "application/warc"
  val CdxMime = "application/cdx"

  def loadCdx[R](spec: InputSpec)(action: RDD[CdxRecord] => R): R = {
    if (InputSpec.isCollectionBased(spec)) {
      spec.collection.specifics.loadCdx(spec.inputPath)(action)
    } else {
      InputSpecLoader.loadSpark(spec) { rdd =>
        action {
          rdd.flatMap { record =>
            if (record.mime == WarcMime) {
              CdxUtil.fromWarcGzStream(record.filename, record.access).map { r =>
                val Seq(offsetStr, _) = r.additionalFields
                r.copy(additionalFields = Seq(offsetStr, record.pointer.url))
              }
            } else if (record.mime == CdxMime) {
              IOUtil.lines(record.access).flatMap(CdxRecord.fromString)
            } else Iterator.empty
          }
        }
      }
    }
  }

  def loadCdxFromWarcGzStreams(rdd: RDD[(FilePointer, InputStream)]): RDD[CdxRecord] = {
    rdd.flatMap { case (file, in) =>
      CdxUtil.fromWarcGzStream(file.filename, in).map { r =>
        val Seq(offsetStr, _) = r.additionalFields
        r.copy(additionalFields = Seq(offsetStr, file.url))
      }
    }
  }

  def loadWarc[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    InputSpecLoader.loadSpark(spec) { rdd =>
      val accessContext = FileAccessContext.fromLocalArchConf
      action({
        spec.inputType match {
          case InputSpec.InputType.WARC | InputSpec.InputType.Files =>
            rdd.mapPartitions { partition =>
              accessContext.init()
              partition.filter(_.mime == WarcMime)
            }
          case InputSpec.InputType.CDX =>
            rdd.mapPartitions { partition =>
              accessContext.init()
              partition.filter(_.mime == CdxMime).map { f =>
                val cdx = IOUtil.lines(f.access).flatMap(CdxRecord.fromString)
                val in = warcFilesViaCdx(cdx) { pointers =>
                  randomAccess(
                    accessContext,
                    pointers.map { case ((pointer, initialOffset), positions) =>
                      (
                        (pointer.relative(f.pointer), initialOffset),
                        positions.map { case (_, o, l) => (o, l) })
                    })
                }
                f.withAccess(in)
              }
            }
          case _ => RddUtil.emptyRDD
        }
      })
    }
  }

  def loadWarcsRecords[R](inputSpec: InputSpec)(
      action: RDD[(FilePointer, CleanupIterator[WarcRecord])] => R): R = {
    loadWarc(inputSpec) { rdd =>
      action(rdd.map { file =>
        val in = IOUtil.supportMark(file.access)
        val warcs = WarcLoader.load(in).filter {
          r => r.isResponse || r.isRevisit
        }
        (
          file.pointer,
          IteratorUtil.cleanup(
            IteratorUtil.tryCatch(warcs),
            in.close))
      })
    }
  }

  def loadWarcs[R](spec: InputSpec)(action: RDD[WarcRecord] => R): R = {
    loadWarcsRecords(spec)(rdd => action(rdd.flatMap(_._2)))
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

  def aitWarcUrl(filename: String): String = {
    ArchConf.aitWarcsBaseUrl + "/webdatafile/" + filename
  }

  def aitCacheBackedInputStream(filename: String, cachePath: String, direct: Option[InputStream] = None, basicAuth: Option[String], location: Option[String] = None, rewriteCache: Boolean): InputStream = {
    new CacheBackedInputStream(
      source = {
        Ait
          .getWithAuth(
            location.getOrElse(aitWarcUrl(filename)),
            contentType = "*/*",
            basicAuth = basicAuth,
            close = false)(Some(_)).toOption.get
      },
      cachePath = Some(cachePath + "/" + filename),
      direct = direct,
      rewriteCache = rewriteCache
    )
  }

  def aitCacheBasedRdd(rdd: RDD[(String, Option[InputStream], Option[String], Boolean)], cachePath: String, basicAuth: Option[String]): RDD[(String, InputStream)] = {
    rdd.map { case (filename, stream, location, isCache) =>
      (filename, aitCacheBackedInputStream(filename, cachePath, stream, basicAuth, location, isCache))
    }.mapPartitions { partition =>
      var prev: Option[InputStream] = None
      partition.map { case (f, in) =>
        for (s <- prev) s.close()
        prev = Some(in)
        (f, prev.get)
      } ++ IteratorUtil.noop {
        for (s <- prev) s.close()
      }
    }
  }

  def aitAccessTuple(filename: String, in: InputStream, isCache: Boolean): (String, Option[InputStream], Option[String], Boolean) = {
    (filename, Some(in), None, isCache)
  }

  def loadAitWarcFiles[R](aitId: Int, inputPath: String, cacheId: String)(
      action: RDD[(String, InputStream)] => R): R = {
    val basicAuth = ArchConf.foreignAitAuthHeader
    val hdfsHostPort = ArchConf.aitCollectionHdfsHostPort
    if (basicAuth.isDefined) {
      val wasapiUrl =
        ArchConf.aitWarcsBaseUrl + "/wasapi/v1/webdata?format=json&filetype=warc&collection=" + aitId + "&page_size="
      var apiFileCount = -1
      var attempts = 0
      var success = false
      while (!success && apiFileCount < 0) {
        attempts += 1
        if (attempts > WasapiAttempts)
          throw new RuntimeException("WASAPI error (AIT collection " + aitId + ")")
        Ait
          .getJsonWithAuth(wasapiUrl + 1, basicAuth = basicAuth) { json =>
            json.get[Int]("count").toOption
          } match {
          case Right(i) =>
            success = true
            apiFileCount = i
          case Left(status) => Thread.sleep(RetrySleepMillis)
        }
      }
      val hdfsFileCount = hdfsHostPort
        .map { case (host, port) =>
          HdfsIO(host, port).files(inputPath + "/*arc.gz", recursive = false).size
        }
        .getOrElse(0)
      CollectionCache.cache(cacheId) { cachePath =>
        if (hdfsFileCount == apiFileCount) {
          loadWarcFiles(inputPath, hdfsHostPort) { rdd =>
            action(aitCacheBasedRdd(rdd.map(t => aitAccessTuple(t._1, t._2, isCache = false)), cachePath, basicAuth))
          }
        } else {
          val cacheFileCount = HdfsIO.files(cachePath + "/*arc.gz", recursive = false).size
          if (hdfsFileCount + cacheFileCount == apiFileCount) {
            if (hdfsHostPort.isDefined) {
              loadWarcFiles(inputPath, hdfsHostPort) { rdd =>
                loadWarcFiles(cachePath) { cachedRdd =>
                  action(aitCacheBasedRdd(rdd.map { case (filename, in) =>
                    aitAccessTuple(filename, in, isCache = false)
                  }.union(cachedRdd.map { case (filename, in) =>
                    aitAccessTuple(filename, in, isCache = true)
                  }), cachePath, basicAuth))
                }
              }
            } else {
              loadWarcFiles(cachePath) { rdd =>
                action(aitCacheBasedRdd(rdd.map(t => aitAccessTuple(t._1, t._2, isCache = true)), cachePath, basicAuth))
              }
            }
          } else {
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
                  val aitHdfsIO = hdfsHostPort.map { case (host, port) => HdfsIO(host, port) }
                  partition
                    .map { case (file, location) =>
                      val inputFilePath = inputPath + "/" + file
                      if (aitHdfsIO.exists(_.exists(inputFilePath))) {
                        (file, location, Some(inputFilePath))
                      } else {
                        (file, location, None)
                      }
                    }
                    .map { case (filename, location, directPath) =>
                      (filename, aitCacheBackedInputStream(filename, cachePath, direct = directPath.map { p =>
                        aitHdfsIO.get.open(p, decompress = false)
                      }, basicAuth, Some(location), rewriteCache = false))
                    }
                }
            }
          }
        }
      }
    } else {
      loadWarcFiles(inputPath, hdfsHostPort)(action)
    }
  }

  private def warcFilesViaCdx(records: Iterator[CdxRecord])(
      in: Iterator[((FilePointer, Long), Iterator[(CdxRecord, Long, Long)])] => Iterator[
        InputStream]): InputStream = {
    val pointers = records.flatMap { cdx =>
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
          group.map { case (r, _, o, l) => (r, o - initialOffset, l) })
      }
    new BufferedInputStream(new ChainedInputStream(in(groups))).asInstanceOf[InputStream]
  }

  private def loadWarcFilesViaCdx(cdxPath: String)(
      in: Iterator[((FilePointer, Long), Iterator[(CdxRecord, Long, Long)])] => Iterator[
        InputStream]): RDD[(String, InputStream)] = {
    val inputPath = s"$cdxPath/*.cdx.gz"
    val numFiles = HdfsIO.files(inputPath, recursive = false).size
    RddUtil
      .loadTextFiles(inputPath)
      .map { case (file, lines) =>
        (file.split('/').last, warcFilesViaCdx(lines.flatMap(CdxRecord.fromString))(in))
      }
      .coalesce(numFiles / WarcFilesPerPartition + 1)
  }

  private def randomAccess(
      context: FileAccessContext,
      pointers: Iterator[((FilePointer, Long), Iterator[(Long, Long)])])
      : Iterator[InputStream] = {
    pointers.map { case ((pointer, initialOffset), positions) =>
      RandomFileAccess.access(context, pointer, initialOffset, positions)
    }
  }

  def loadWarcFilesViaCdxFiles(cdxPath: String): RDD[(String, InputStream)] = {
    val accessContext = FileAccessContext.fromLocalArchConf
    loadWarcFilesViaCdx(cdxPath) { pointers =>
      accessContext.init()
      randomAccess(
        accessContext,
        pointers.map { case ((pointer, initialOffset), positions) =>
          ((pointer, initialOffset), positions.map { case (_, o, l) => (o, l) })
        })
    }
  }

  def loadWarcFilesViaCdxFromCollections(
      cdxPath: String,
      collectionId: String): RDD[(String, InputStream)] = {
    val accessContext = FileAccessContext.fromLocalArchConf
    loadWarcFilesViaCdx(cdxPath) { partition =>
      accessContext.init()
      CollectionSpecifics.get(collectionId).toIterator.flatMap { specifics =>
        partition.map { case ((pointer, initialOffset), positions) =>
          specifics.randomAccess(
            accessContext,
            specifics.inputPath,
            pointer,
            initialOffset,
            positions.map { case (_, o, l) => (o, l) })
        }
      }
    }
  }

  def randomAccessHdfs(
      context: FileAccessContext,
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
    val accessContext = FileAccessContext.fromLocalArchConf(alwaysAitHdfsIO = aitHdfs)
    loadWarcFilesViaCdx(cdxPath) { partition =>
      accessContext.init()
      partition.map { case ((pointer, initialOffset), positions) =>
        randomAccessHdfs(
          accessContext,
          warcPath + "/" + pointer.filename,
          initialOffset,
          positions.map { case (_, o, l) => (o, l) })
      }
    }
  }

  def randomAccessAit(
      context: FileAccessContext,
      sourceId: String,
      filePath: String,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    if (context.aitHdfsIOopt.exists(_.exists(filePath))) {
      val in = context.aitHdfsIOopt.get.open(
        filePath,
        offset = offset,
        decompress = false,
        strategy = HdfsIO.LoadingStrategy.Remote)
      IOHelper.splitMergeInputStreams(in, positions, buffered = false)
    } else {
      val file = filePath.split('/').last
      val cachePath = CollectionCache.cachePath(sourceId, file)
      if (HdfsIO.exists(cachePath)) {
        val in = HdfsIO.open(
          cachePath,
          offset = offset,
          decompress = false,
          strategy = HdfsIO.LoadingStrategy.Remote)
        IOHelper.splitMergeInputStreams(in, positions, buffered = false)
      } else {
        HttpClient.rangeRequest(
          aitWarcUrl(file),
          headers = ArchConf.foreignAitAuthHeader.map("Authorization" -> _).toMap,
          offset = offset) { in =>
          IOHelper.splitMergeInputStreams(in, positions)
        }
      }
    }
  }
}

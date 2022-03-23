package org.archive.webservices.ars.io

import java.io.InputStream

import org.apache.spark.rdd.RDD
import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil}
import org.archive.webservices.sparkling.util.{CleanupIterator, Common, IteratorUtil, RddUtil}
import org.archive.webservices.sparkling.warc.{WarcLoader, WarcRecord}
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.collections.CollectionSpecifics

object CollectionLoader {
  val WasapiPageSize = 100
  val TargetPartitions = 1000
  val RetrySleepMillis = 5000

  def loadWarcFiles(id: String, inputPath: String): RDD[(String, InputStream)] = {
    CollectionSpecifics.get(id) match {
      case Some(specifics) =>
        specifics.loadWarcFiles(inputPath)
      case None => loadWarcFiles(inputPath)
    }
  }.coalesce(TargetPartitions)

  def loadWarcsWithPath(
      id: String,
      inputPath: String): RDD[(String, CleanupIterator[WarcRecord])] = {
    loadWarcFiles(id, inputPath).map {
      case (p, in) =>
        (
          p,
          IteratorUtil.cleanup(
            WarcLoader
              .load(in)
              .filter(r => r.isResponse || r.isRevisit),
            in.close))
    }
  }

  def loadWarcs(id: String, inputPath: String): RDD[WarcRecord] =
    loadWarcsWithPath(id, inputPath).mapPartitions(_.flatMap(_._2))

  def loadWarcFiles(
      inputPath: String,
      hdfsHostPort: Option[(String, Int)] = None): RDD[(String, InputStream)] = {
    (hdfsHostPort match {
      case Some((host, port)) =>
        RddUtil
          .parallelize(HdfsIO(host, port).files(inputPath + "/*.warc.gz").toSeq)
          .mapPartitions { paths =>
            val hdfsIO = HdfsIO(host, port)
            paths.map(p => (p, hdfsIO.open(p, decompress = false)))
          }
      case None =>
        RddUtil
          .loadFilesLocality(inputPath + "/*.warc.gz")
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

  def loadAitWarcFiles(
      aitId: Int,
      inputPath: String,
      cacheId: String): RDD[(String, InputStream)] = {
    val basicAuth = ArchConf.foreignAitAuthHeader
    val hdfsHostPort = ArchConf.aitCollectionHdfsHostPort
    if (basicAuth.isDefined) {
      val wasapiUrl = "https://warcs.archive-it.org/wasapi/v1/webdata?format=json&collection=" + aitId + "&page_size="
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
        .files(inputPath + "/*.warc.gz", recursive = false)
        .size
      if (hdfsFileCount == apiFileCount) {
        loadWarcFiles(inputPath, hdfsHostPort)
      } else {
        CollectionCache.cache(cacheId) { cachePath =>
          val cacheFileCount = HdfsIO.files(cachePath + "/*.warc.gz", recursive = false).size
          if (hdfsFileCount + cacheFileCount == apiFileCount) {
            loadWarcFiles(inputPath, hdfsHostPort)
              .union(loadWarcFiles(cachePath))
          } else {
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
              .repartition(TargetPartitions)
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
                                        overwrite = true)
                                    try {
                                      IOUtil.copy(in, out)
                                      Some((inputFilePath, cacheFilePath, false))
                                    } finally {
                                      out.close()
                                    }
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
      loadWarcFiles(inputPath, hdfsHostPort)
    }
  }
}

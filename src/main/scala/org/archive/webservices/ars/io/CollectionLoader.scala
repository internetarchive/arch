package org.archive.webservices.ars.io

import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.io.{HdfsIO, IOUtil}
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.helge.sparkling.warc.{WarcLoader, WarcRecord}
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.collections.CollectionSpecifics

object CollectionLoader {
  val WasapiPageSize = 10

  def loadWarcs(
      inputPath: String,
      hdfsHostPort: Option[(String, Int)] = None): RDD[WarcRecord] = {
    (hdfsHostPort match {
      case Some((host, port)) =>
        RddUtil
          .parallelize(HdfsIO(host, port).files(inputPath + "/*.warc.gz").toSeq)
          .mapPartitions { paths =>
            val hdfsIO = HdfsIO(host, port)
            paths.map(hdfsIO.open(_, decompress = false))
          }
      case None =>
        RddUtil
          .loadFilesLocality(inputPath + "/*.warc.gz")
          .map(HdfsIO.open(_, decompress = false))
    }).flatMap { in =>
      IteratorUtil.cleanup(
        WarcLoader
          .load(in)
          .filter(r => r.isResponse || r.isRevisit),
        in.close)
    }
  }

  def loadWarcs(id: String, inputPath: String): RDD[WarcRecord] =
    CollectionSpecifics.get(id) match {
      case Some(specifics) =>
        specifics.loadWarcs(inputPath)
      case None => loadWarcs(inputPath)
    }

  def loadAitWarcs(aitId: Int, inputPath: String, cacheId: String): RDD[WarcRecord] = {
    val basicAuth = ArchConf.foreignAitAuthHeader
    val hdfsHostPort = ArchConf.aitCollectionHdfsHostPort
    if (basicAuth.isDefined) {
      val wasapiUrl = "https://warcs.archive-it.org/wasapi/v1/webdata?format=json&collection=" + aitId + "&page_size="
      val apiFileCount = Ait
        .getJsonWithAuth(wasapiUrl + 1, basicAuth = basicAuth) { json =>
          json.get[Int]("count").toOption
        }
        .getOrElse(0)
      val hdfsFileCount = hdfsHostPort
        .map { case (host, port) => HdfsIO(host, port) }
        .getOrElse(HdfsIO)
        .files(inputPath + "/*.warc.gz", recursive = false)
        .size
      if (hdfsFileCount == apiFileCount) {
        loadWarcs(inputPath, hdfsHostPort)
      } else {
        CollectionCache.cache(cacheId) { cachePath =>
          val cacheFileCount = HdfsIO.files(cachePath + "/*.warc.gz", recursive = false).size
          if (hdfsFileCount + cacheFileCount == apiFileCount) {
            loadWarcs(inputPath, hdfsHostPort)
              .union(loadWarcs(cachePath))
          } else {
            RddUtil
              .parallelize((apiFileCount.toDouble / WasapiPageSize).ceil.toInt)
              .flatMap { idx =>
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
                  }
                  .getOrElse(Iterator.empty)
              }
              .mapPartitions { partition =>
                val hdfsIO = hdfsHostPort
                  .map { case (host, port) => HdfsIO(host, port) }
                  .getOrElse(HdfsIO)
                partition
                  .flatMap {
                    case (file, location) =>
                      val inputFilePath = inputPath + "/" + file
                      if (hdfsIO.exists(inputFilePath)) Some((inputFilePath, true))
                      else {
                        val cacheFilePath = cachePath + "/" + file
                        if (HdfsIO.exists(cacheFilePath)) Some((cacheFilePath, false))
                        else {
                          IOHelper.syncHdfs(cacheFilePath + "_caching") {
                            if (HdfsIO.exists(cacheFilePath)) Some((cacheFilePath, false))
                            else {
                              Ait.getWithAuth(
                                location,
                                contentType = "*/*",
                                basicAuth = basicAuth) { in =>
                                val out =
                                  HdfsIO.out(cacheFilePath, compress = false, overwrite = true)
                                try {
                                  IOUtil.copy(in, out)
                                  Some((cacheFilePath, false))
                                } finally {
                                  out.close()
                                }
                              }
                            }
                          }
                        }
                      }
                  }
                  .flatMap {
                    case (path, ait) =>
                      val in = (if (ait) hdfsIO else HdfsIO).open(path, decompress = false)
                      IteratorUtil.cleanup(
                        WarcLoader
                          .load(in)
                          .filter(r => r.isResponse || r.isRevisit),
                        in.close)
                  }
              }
          }
        }
      }
    } else {
      loadWarcs(inputPath, hdfsHostPort)
    }
  }
}

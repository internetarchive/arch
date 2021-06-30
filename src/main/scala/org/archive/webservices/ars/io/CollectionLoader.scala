package org.archive.webservices.ars.io

import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.io.{HdfsIO, IOUtil}
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.helge.sparkling.warc.{WarcLoader, WarcRecord}
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.model.ArsCloudConf
import org.archive.webservices.ars.model.collections.{AitCollectionSpecifics, CollectionSpecifics}

object CollectionLoader {
  def loadWarcs(inputPath: String): RDD[WarcRecord] = {
    RddUtil
      .loadBinary(inputPath + "/*arc.gz", decompress = false, close = false) { (filename, in) =>
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
    val basicAuth = ArsCloudConf.aitAuthHeader
    if (basicAuth.isDefined) {
      val wasapiUrl = "https://warcs.archive-it.org/wasapi/v1/webdata?format=json&collection=" + aitId + "&page_size="
      val fileCount = Ait
        .getJsonWithAuth(wasapiUrl + 1, basicAuth = basicAuth) { json =>
          json.get[Int]("count").toOption
        }
        .getOrElse(0)
      val actualFileCount = HdfsIO.files(inputPath + "/*.warc.gz", recursive = false).size
      if (actualFileCount == fileCount) {
        loadWarcs(inputPath)
      } else {
        CollectionCache.cache(cacheId) { cachePath =>
          val cacheFileCount = HdfsIO.files(cachePath + "/*.warc.gz", recursive = false).size
          if (actualFileCount + cacheFileCount == fileCount) {
            loadWarcs(inputPath).union(loadWarcs(cachePath))
          } else {
            RddUtil
              .parallelize(
                (fileCount.toDouble / AitCollectionSpecifics.WasapiPageSize).ceil.toInt)
              .flatMap { idx =>
                Ait
                  .getJsonWithAuth(
                    wasapiUrl + AitCollectionSpecifics.WasapiPageSize + "&page=" + (idx + 1),
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
              .flatMap {
                case (file, location) =>
                  val inputFilePath = inputPath + "/" + file
                  if (HdfsIO.exists(inputFilePath)) Some(inputFilePath)
                  else {
                    val cacheFilePath = cachePath + "/" + file
                    if (HdfsIO.exists(cacheFilePath)) Some(cacheFilePath)
                    else {
                      IOHelper.syncHdfs(cachePath + "/_caching") {
                        if (HdfsIO.exists(cacheFilePath)) Some(cacheFilePath)
                        else {
                          Ait.getWithAuth(location, contentType = "*/*", basicAuth = basicAuth) {
                            in =>
                              val out =
                                HdfsIO.out(cacheFilePath, compress = false, overwrite = true)
                              try {
                                IOUtil.copy(in, out)
                                Some(cacheFilePath)
                              } finally {
                                out.close()
                              }
                          }
                        }
                      }
                    }
                  }
              }
              .flatMap { path =>
                val in = HdfsIO.open(path, decompress = false)
                IteratorUtil.cleanup(
                  WarcLoader
                    .load(in)
                    .filter(r => r.isResponse || r.isRevisit),
                  in.close)
              }
          }
        }
      }
    } else {
      loadWarcs(inputPath)
    }
  }
}

package org.archive.webservices.ars.processing.jobs

import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.io.{CollectionLoader, IOHelper}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.shared.ArsJob
import org.archive.webservices.ars.util.HttpUtil
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.ars.WANE
import org.archive.webservices.sparkling.html.HtmlProcessor
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.util.{RddUtil, StringUtil}

import scala.concurrent.Future
import scala.util.Try

object ArsWaneGeneration extends SparkJob with ArsJob {
  val MaxInputTextLength = 10000

  val name = "Named entities"
  val uuid = "01895065-8f59-7a8a-b432-79e20d749f4a"
  val category: ArchJobCategory = ArchJobCategories.Text
  def description =
    "Names of persons, organizations, and geographic locations detected in each text-bearing document in the collection. Output: one WANE file with data in JSON format for each WARC file."

  val relativeOutPath = s"/$id"
  val resultDir = "/wane.gz"

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { sc =>
      SparkJobManager.initThread(sc, ArsWaneGeneration, conf)
      CollectionLoader.loadWarcsWithSource(conf.collectionId, conf.inputPath) { rdd =>
        IOHelper
          .sampleGrouped[String, String, Boolean](
            rdd
              .map { case (pointer, records) =>
                (
                  new Path(pointer.filename).getName,
                  records.chain(_.filter(_.http.exists(http =>
                    http.mime.contains("text/html") && http.status == 200))))
              }
              .map { case (f, r) =>
                val outFile = StringUtil.stripSuffix(f, Sparkling.GzipExt) + ".wane.gz"
                val json = r.chain(
                  _.flatMap { warc =>
                    for {
                      url <- warc.url
                      timestamp <- warc.timestamp
                      digest <- warc.payloadDigest
                      http <- warc.http if http.status == 200
                      html <- Try(HtmlProcessor.strictHtml(HttpUtil.bodyString(http.body, http)))
                        .getOrElse(None)
                      bodyText <- Try(
                        HtmlProcessor
                          .tagsWithText(html, Set("body"))
                          .next
                          ._2
                          .take(MaxInputTextLength)).toOption
                    } yield WANE.get(url, timestamp, digest, bodyText)
                  }.filter(e =>
                    e.locations.nonEmpty || e.organizations.nonEmpty || e.persons.nonEmpty)
                    .map(_.toJsonString))
                (outFile, json)
              },
            conf.sample) { rdd =>
            val outPath = conf.outputPath + relativeOutPath + resultDir
            val processed = RddUtil.saveGroupedAsNamedTextFile(
              rdd.map { case (k, v) => (k, v) },
              outPath,
              skipIfExists = true)
            RddUtil.loadFilesLocality(outPath + "/*.wane.gz").foreachPartition { files =>
              for (file <- files) DerivativeOutput.hashFileHdfs(file)
            }
            processed >= 0
          }
      }
    }
  }

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    val started = HdfsIO.exists(conf.outputPath + relativeOutPath + resultDir)
    if (started) {
      val completed =
        HdfsIO.exists(
          conf.outputPath + relativeOutPath + resultDir + "/" + Sparkling.CompleteFlagFile)
      instance.state = if (completed) ProcessingState.Finished else ProcessingState.Failed
    }
    instance
  }

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] =
    HdfsIO.files(conf.outputPath + relativeOutPath + resultDir + "/*.gz").map { file =>
      val (path, name) = file.splitAt(file.lastIndexOf('/'))
      DerivativeOutput(name.stripPrefix("/"), path, "wane", "application/gzip")
    }

  override val templateName: Option[String] = Some("jobs/DefaultArsJob")

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)
}

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
import org.archive.webservices.sparkling.warc.WarcRecord

import scala.concurrent.Future
import scala.util.Try

object ArsWaneGeneration extends SparkJob with ArsJob {
  val MaxInputTextLength = 10000

  val name = "Generate WANE files"
  val category: ArchJobCategory = ArchJobCategories.Text
  def description = "Generate WANE (Named Entities)"

  val relativeOutPath = s"/$id"
  val resultDir = "/wane.gz"

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { _ =>
      IOHelper
        .sampleGrouped[String, WarcRecord, Boolean](
          CollectionLoader.loadWarcsWithPath(conf.collectionId, conf.inputPath).map {
            case (path, records) =>
              (
                new Path(path).getName,
                records.chain(_.filter(_.http.exists(http =>
                  http.mime.contains("text/html") && http.status == 200))))
          },
          conf.sample) { rdd =>
          val outPath = conf.outputPath + relativeOutPath + resultDir
          val processed =
            RddUtil.saveGroupedAsNamedTextFile(
              rdd.map {
                case (f, r) =>
                  val outFile = StringUtil.stripSuffix(f, Sparkling.GzipExt) + ".wane.gz"
                  val json = r.flatMap {
                    warc =>
                      for {
                        url <- warc.url
                        timestamp <- warc.timestamp
                        digest <- warc.payloadDigest
                        http <- warc.http if http.status == 200
                        html <- Try(
                          HtmlProcessor.strictHtml(HttpUtil.bodyString(http.body, http)))
                          .getOrElse(None)
                        bodyText <- Try(
                          HtmlProcessor
                            .tagsWithText(html, Set("body"))
                            .next
                            ._2
                            .take(MaxInputTextLength)).toOption
                      } yield WANE.get(url, timestamp, digest, bodyText).toJsonString
                  }
                  (outFile, json)
              },
              outPath,
              skipIfExists = true)
          processed > 0
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

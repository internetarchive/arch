package org.archive.webservices.ars.processing.jobs.shared

import io.archivesunleashed.matchbox.GetExtensionMIME
import org.apache.commons.io.FilenameUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.webservices.ars.aut.{AutLoader, AutUtil, TikaUtil}
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing.{DerivationJobConf, ProcessingState}
import org.archive.webservices.ars.util.Common
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.http.HttpMessage
import org.archive.webservices.sparkling.io.{HdfsIO, InputStreamForker}
import org.archive.webservices.sparkling.util.{DigestUtil, RddUtil}
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.{InputStream, PrintStream}
import java.net.URL
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

object BinaryInformationAutJob {
  val MimeTypeCountFile: String = "mime-type-count.csv.gz"
  val MimeTypeColumn: String = "mime_type_web_server"
}

abstract class BinaryInformationAutJob extends AutJob[Row] {
  import BinaryInformationAutJob._

  val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date, last_modified_date, url, filename, extension, mime_type_web_server, mime_type_tika, md5, sha1")

  def checkMime(url: String, server: String, tika: String): Boolean

  override def df(rdd: RDD[Row]): Dataset[Row] = AutLoader.binaryInformation(rdd)

  protected def computeMimeTypeCounts(dataset: Dataset[Row], outPath: String): Unit = {
    RddUtil.saveAsTextFile(
      dataset.rdd
        .map(r => (r.getAs[String](MimeTypeColumn), 1L))
        .reduceByKey(_ + _)
        .sortBy(-_._2)
        .map { case (m, c) => m + "," + c },
      outPath + "/_" + MimeTypeCountFile)
  }

  override def runSpark(rdd: RDD[Row], outPath: String): Unit = {
    val data = AutLoader.saveAndLoad(df(rdd), outPath + "/_" + targetFile)

    HdfsIO.writeLines(
      outPath + "/" + targetFile + DerivativeOutput.LineCountFileSuffix,
      Seq(data.count.toString),
      overwrite = true)

    computeMimeTypeCounts(data, outPath)
  }

  def prepareBinaryRow(
      r: WarcRecord,
      row: (String, HttpMessage, InputStream, String, String, String) => Row): Option[Row] = {
    Common.tryOrElse[Option[Row]](None) {
      r.http.filter(_.status == 200).flatMap { http =>
        val url = AutUtil.url(r)
        val body = http.body
        val lastModifiedDate =
          AutUtil.rfc1123toTime14(http.headerMap.get("last-modified").getOrElse(""))
        val tikaMime = TikaUtil.mime(body)
        if (checkMime(url, http.mime.getOrElse(""), tikaMime)) {
          val crawlDate = AutUtil.timestamp(r)
          if (crawlDate.nonEmpty) Some {
            row(url, http, body, tikaMime, crawlDate, lastModifiedDate)
          }
          else None
        } else None
      }
    }
  }

  def prepareRecord(r: WarcRecord): Option[Row] =
    prepareBinaryRow(
      r,
      (
          url: String,
          http: HttpMessage,
          body: InputStream,
          tikaMime: String,
          crawlDate: String,
          lastModifiedDate: String) => {
        val forker = InputStreamForker(body)
        val Array(md5In, sha1In) = forker.fork(2).map(Future(_))
        val Seq(md5, sha1) =
          try {
            Await.result(
              Future.sequence(Seq(md5In.map(DigestUtil.md5Hex), sha1In.map(DigestUtil.sha1Hex))),
              Duration.Inf)
          } finally {
            for (s <- md5In) Try(s.close())
            for (s <- sha1In) Try(s.close())
            Try(body.close())
          }

        val jUrl = new URL(url)
        val filename = FilenameUtils.getName(jUrl.getPath)
        val extension = GetExtensionMIME(jUrl.getPath, tikaMime)

        Row(
          crawlDate,
          lastModifiedDate,
          url,
          filename,
          extension,
          AutUtil.mime(http),
          tikaMime,
          md5,
          sha1)
      })

  override def checkSparkState(outPath: String): Option[Int] =
    super.checkSparkState(outPath).map { state =>
      if (HdfsIO.exists(outPath + "/_" + MimeTypeCountFile + "/" + Sparkling.CompleteFlagFile))
        state
      else ProcessingState.Failed
    }

  protected def postProcessMimeTypeCounts(outPath: String): Boolean = {
    IOHelper.concatLocal(
      outPath + "/_" + MimeTypeCountFile,
      _.endsWith(".csv.gz"),
      decompress = false,
      deleteSrcFiles = true,
      deleteSrcPath = true) { tmpFile =>
      val outFile = outPath + "/" + MimeTypeCountFile
      HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
      HdfsIO.exists(outFile)
    }
  }

  override def postProcess(outPath: String): Boolean =
    super.postProcess(outPath) && postProcessMimeTypeCounts(outPath)

  override def checkFinishedState(outPath: String): Option[Int] =
    super.checkFinishedState(outPath).map { state =>
      if (HdfsIO.exists(outPath + "/" + MimeTypeCountFile)) state else ProcessingState.Failed
    }

  override val templateName: Option[String] = Some("jobs/BinaryInformationExtraction")

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    val mimeCount = HdfsIO
      .lines(conf.outputPath + relativeOutPath + "/" + MimeTypeCountFile, n = 5)
      .flatMap { line =>
        val comma = line.lastIndexOf(',')
        if (comma < 0) None
        else
          Some {
            val (mimeType, count) =
              (line.take(comma).stripPrefix("\"").stripSuffix("\""), line.drop(comma + 1))
            (mimeType, count.toInt)
          }
      }
    super.templateVariables(conf) ++ Seq("mimeCount" -> mimeCount)
  }
}

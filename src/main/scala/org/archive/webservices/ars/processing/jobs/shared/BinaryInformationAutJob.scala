package org.archive.webservices.ars.processing.jobs.shared

import java.io.{InputStream, PrintStream}
import java.net.URL

import io.archivesunleashed.matchbox.GetExtensionMIME
import org.apache.commons.io.FilenameUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.helge.sparkling.Sparkling
import org.archive.helge.sparkling.Sparkling.executionContext
import org.archive.helge.sparkling.http.HttpMessage
import org.archive.helge.sparkling.io.{HdfsIO, InputStreamForker}
import org.archive.helge.sparkling.util.{DigestUtil, RddUtil}
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.aut.{AutLoader, AutUtil, TikaUtil}
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing.{DerivationJobConf, ProcessingState}
import org.archive.webservices.ars.util.Common

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

abstract class BinaryInformationAutJob extends AutJob[Row] {
  val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  val MimeTypeCountFile: String = "mime-type-count.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println("crawl_date,url,filename,extension,mime_type_web_server,mime_type_tika,md5,sha1")

  def checkMime(url: String, server: String, tika: String): Boolean

  override def df(rdd: RDD[Row]): Dataset[Row] = AutLoader.binaryInformation(rdd)

  protected def computeMimeTypeCounts(dataset: Dataset[Row], outPath: String): Unit = {
    RddUtil.saveAsTextFile(
      dataset.rdd
        .map(r => (r.getAs[String]("mime_type_web_server"), 1L))
        .reduceByKey(_ + _)
        .sortBy(-_._2)
        .map { case (m, c) => m + "," + c },
      outPath + "/_" + MimeTypeCountFile)
  }

  override def runSpark(rdd: RDD[Row], outPath: String): Unit = {
    val data = AutLoader.saveAndLoad(df(rdd), outPath + "/_" + targetFile)

    HdfsIO.writeLines(
      outPath + "/" + targetFile + DerivativeOutput.lineCountFileSuffix,
      Seq(data.count.toString),
      overwrite = true)

    computeMimeTypeCounts(data, outPath)
  }

  def row(
      url: String,
      http: HttpMessage,
      body: InputStream,
      tikaMime: String,
      crawlDate: String): Row = {
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

    Row(crawlDate, url, filename, extension, AutUtil.mime(http), tikaMime, md5, sha1)
  }

  def prepareRecord(r: WarcRecord): Option[Row] = {
    Common.tryOrElse[Option[Row]](None) {
      r.http.filter(_.status == 200).flatMap { http =>
        val url = AutUtil.url(r)
        val body = http.body
        val tikaMime = TikaUtil.mime(body)
        if (checkMime(url, http.mime.getOrElse(""), tikaMime)) {
          val crawlDate = AutUtil.crawlDate(r)
          if (crawlDate.nonEmpty) Some {
            row(url, http, body, tikaMime, crawlDate)
          } else None
        } else None
      }
    }
  }

  override def checkSparkState(outPath: String): Option[Int] =
    super.checkSparkState(outPath).map { state =>
      if (HdfsIO.exists(outPath + "/_" + MimeTypeCountFile + "/" + Sparkling.CompleteFlagFile))
        state
      else ProcessingState.Failed
    }

  protected def postProcessMimeTypeCounts(outPath: String): Boolean = {
    IOHelper.concatLocal(
      outPath + "/_" + MimeTypeCountFile,
      MimeTypeCountFile,
      _.endsWith(".csv.gz"),
      compress = true,
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

  override def templateName: Option[String] = Some("jobs/BinaryInformationExtraction")

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

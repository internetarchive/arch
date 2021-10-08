package org.archive.webservices.ars.processing.jobs

import java.io.{InputStream, OutputStream, PrintStream}
import java.net.URL

import io.archivesunleashed.matchbox.{GetExtensionMIME, RemoveHTTPHeader}
import org.apache.commons.io.FilenameUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.apache.spark.sql.functions.{col, desc}
import org.archive.helge.sparkling.Sparkling.executionContext
import org.archive.helge.sparkling.http.HttpMessage
import org.archive.helge.sparkling.io.{HdfsIO, InputStreamForker}
import org.archive.helge.sparkling.util.{Common, DigestUtil}
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.util.HttpUtil
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

object TextFilesInformationExtraction extends BinaryInformationAutJob {
  override val category: ArchJobCategory = ArchJobCategories.Text

  val name = "Extract text files (html, text, css, js, json, xml) information"

  val description =
    "Create a CSV with the following columns: crawl date, URL of the text file, filename, text extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, text file MD5 hash and text file SHA1 hash, and text file content."

  val targetFile: String = "file-information.csv.gz"

  val textFilesJobs: Map[String, String] = Map(
    "css" -> "text/css",
    "html" -> "text/html",
    "js" -> "javascript",
    "json" -> "json",
    "plain-text" -> "text/plain",
    "xml" -> "xml")

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date,url,filename,extension,mime_type_web_server,mime_type_tika,md5,sha1,content")

  override def checkSparkState(outPath: String): Option[Int] = {
    if (textFilesJobs.forall {
          case (prefix, _) => HdfsIO.exists(outPath + "/_" + prefix + "-" + targetFile)
        }) Some {
      if (textFilesJobs.forall {
            case (prefix, _) =>
              HdfsIO.exists(outPath + "/_" + prefix + "-" + targetFile + "/_SUCCESS")
          }) ProcessingState.Finished
      else ProcessingState.Failed
    } else None
  }.map { state =>
    if (!HdfsIO.exists(outPath + "/_" + mimeTypeCountFile + "/_SUCCESS")) ProcessingState.Failed
    else state
  }

  override def prepareOutputStream(out: OutputStream): Unit =
    printToOutputStream(new PrintStream(out, true, "utf-8"))

  override def checkMime(url: String, server: String, tika: String): Boolean =
    textFilesJobs.values.exists(server.contains)

  override def df(rdd: RDD[Row]): Dataset[Row] = AutLoader.textFiles(rdd)

  override def runSpark(rdd: RDD[Row], outPath: String): Unit = {
    val targetData = df(rdd).cache

    for ((jobPrefix, mimeTypePattern) <- textFilesJobs) {
      val data = AutLoader.saveAndLoad(
        targetData.filter(col("mime_type_web_server").contains(mimeTypePattern)),
        outPath + "/_" + jobPrefix + "-" + targetFile)

      val lineCount = data
        .count()

      HdfsIO.writeLines(
        outPath + "/" + jobPrefix + "-" + targetFile + DerivativeOutput.lineCountFileSuffix,
        Seq(lineCount.toString),
        overwrite = true)
    }

    val lineCount = targetData
      .count()

    HdfsIO.writeLines(
      outPath + "/" + targetFile + DerivativeOutput.lineCountFileSuffix,
      Seq(lineCount.toString),
      overwrite = true)

    val derivative = targetData
      .groupBy("mime_type_web_server")
      .count()
      .orderBy(desc("count"))
      .limit(5)

    AutLoader.save(derivative, outPath + "/_" + mimeTypeCountFile)

    targetData.unpersist(true)
  }

  override def row(
      url: String,
      http: HttpMessage,
      body: InputStream,
      tikaMime: String,
      crawlDate: String): Row = {
    val forker = InputStreamForker(body)
    val Array(md5In, sha1In, contentIn) = forker.fork(3).map(Future(_))
    val Seq(md5, sha1, content) =
      try {
        Await.result(
          Future.sequence(
            Seq(
              md5In.map(DigestUtil.md5Hex),
              sha1In.map(DigestUtil.sha1Hex),
              contentIn.map(in =>
                Common.cleanup(RemoveHTTPHeader(HttpUtil.bodyString(in, http)))(in.close)))),
          Duration.Inf)
      } finally {
        for (s <- md5In) Try(s.close())
        for (s <- sha1In) Try(s.close())
        for (s <- contentIn) Try(s.close())
        Try(body.close())
      }

    val jUrl = new URL(url)
    val filename = FilenameUtils.getName(jUrl.getPath)
    val extension = GetExtensionMIME(jUrl.getPath, tikaMime)

    Row(crawlDate, url, filename, extension, AutUtil.mime(http), tikaMime, md5, sha1, content)
  }

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)

  override def postProcess(outPath: String): Boolean = super.postProcess(outPath) && {
    for ((jobPrefix, mimeTypePattern) <- textFilesJobs) {
      IOHelper.concatLocal(
        outPath + "/_" + jobPrefix + "-" + targetFile,
        jobPrefix + "-" + targetFile,
        _.startsWith("part-"),
        compress = true,
        deleteSrcFiles = true,
        deleteSrcPath = true,
        prepare = prepareOutputStream) { tmpFile =>
        val outFile = outPath + "/" + jobPrefix + "-" + targetFile
        HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
        HdfsIO.exists(outFile)
      }
    }
    textFilesJobs.forall {
      case (prefix, _) => HdfsIO.exists(outPath + "/" + prefix + "-" + targetFile)
    }
  }

  override def outFiles(conf: DerivationJobConf): Seq[DerivativeOutput] =
    textFilesJobs.keys.toSeq.map(
      p =>
        DerivativeOutput(
          p + "-" + targetFile,
          conf.outputPath + relativeOutPath,
          "application/gzip"))
}

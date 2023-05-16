package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.matchbox.GetExtensionMIME
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.FilenameUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{Dataset, Row}
import org.apache.spark.storage.StorageLevel
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.ars.util.HttpUtil
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.http.HttpMessage
import org.archive.webservices.sparkling.io.{HdfsIO, InputStreamForker}
import org.archive.webservices.sparkling.util.{Common, DigestUtil}
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.{InputStream, OutputStream, PrintStream}
import java.net.URL
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

object TextFilesInformationExtraction extends BinaryInformationAutJob {
  val MimeTypeColumnIndex: Int = 4

  override val category: ArchJobCategory = ArchJobCategories.Text

  val name = "Extract text files (html, text, css, js, json, xml) information"

  val description =
    "Create a CSV with the following columns: crawl date, last modified date, URL of the text file, filename, text extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, text file MD5 hash and text file SHA1 hash, and text file content."

  val targetFile: String = "file-information.csv.gz"

  val TextTypes: Map[String, String] = Map(
    "css" -> "text/css",
    "html" -> "text/html",
    "js" -> "javascript",
    "json" -> "json",
    "plain-text" -> "text/plain",
    "xml" -> "xml")

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date,last_modified_date,url,filename,extension,mime_type_web_server,mime_type_tika,md5,sha1,content")

  override def checkSparkState(outPath: String): Option[Int] = {
    if (TextTypes.forall {
          case (prefix, _) => HdfsIO.exists(outPath + "/_" + prefix + "-" + targetFile)
        }) Some {
      if (TextTypes.forall {
            case (prefix, _) =>
              HdfsIO.exists(outPath + "/_" + prefix + "-" + targetFile + "/_SUCCESS")
          }) ProcessingState.Finished
      else ProcessingState.Failed
    } else None
  }.map { state =>
    if (HdfsIO.exists(outPath + "/_" + MimeTypeCountFile + "/" + Sparkling.CompleteFlagFile))
      state
    else ProcessingState.Failed
  }

  override def prepareOutputStream(out: OutputStream): Unit =
    printToOutputStream(new PrintStream(out, true, "utf-8"))

  override def checkMime(url: String, server: String, tika: String): Boolean =
    TextTypes.values.exists(server.contains)

  override val samplingConditions: Seq[Row => Boolean] = TextTypes.values
    .map(t => (r: Row) => r.getString(MimeTypeColumnIndex).contains(t))
    .toSeq

  override def df(rdd: RDD[Row]): Dataset[Row] = AutLoader.textFiles(rdd)

  override def runSpark(rdd: RDD[Row], outPath: String): Unit = {
    val cachedRdd = rdd.persist(StorageLevel.DISK_ONLY)
    val targetData = df(cachedRdd)

    for ((jobPrefix, mimeTypePattern) <- TextTypes) {
      val data = AutLoader.saveAndLoad(
        targetData.filter(col("mime_type_web_server").contains(mimeTypePattern)),
        outPath + "/_" + jobPrefix + "-" + targetFile)

      HdfsIO.writeLines(
        outPath + "/" + jobPrefix + "-" + targetFile + DerivativeOutput.LineCountFileSuffix,
        Seq(data.count.toString),
        overwrite = true)
    }

    computeMimeTypeCounts(targetData, outPath)

    cachedRdd.unpersist(true)
  }

  override def prepareRecord(r: WarcRecord): Option[Row] =
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
        val Array(md5In, sha1In, contentIn) = forker.fork(3).map(Future(_))
        val Seq(md5, sha1, content) =
          try {
            Await.result(
              Future.sequence(
                Seq(
                  md5In.map(DigestUtil.md5Hex),
                  sha1In.map(DigestUtil.sha1Hex),
                  contentIn.map(in => Common.cleanup(HttpUtil.bodyString(in, http))(in.close)))),
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

        Row(
          crawlDate,
          lastModifiedDate,
          url,
          filename,
          extension,
          AutUtil.mime(http),
          tikaMime,
          md5,
          sha1,
          content)
      })

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)

  override def postProcess(outPath: String): Boolean = postProcessMimeTypeCounts(outPath) && {
    for ((jobPrefix, mimeTypePattern) <- TextTypes) {
      IOHelper.concatLocal(
        outPath + "/_" + jobPrefix + "-" + targetFile,
        _.startsWith("part-"),
        decompress = false,
        deleteSrcFiles = true,
        deleteSrcPath = true,
        prepare = { out =>
          val gzip = new GzipCompressorOutputStream(out)
          prepareOutputStream(gzip)
          gzip.finish()
        }) { tmpFile =>
        val outFile = outPath + "/" + jobPrefix + "-" + targetFile
        DerivativeOutput.hashFileLocal(tmpFile, outFile)
        HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
        HdfsIO.exists(outFile)
      }
    }
    TextTypes.forall {
      case (prefix, _) => HdfsIO.exists(outPath + "/" + prefix + "-" + targetFile)
    }
  }

  override def checkFinishedState(outPath: String): Option[Int] =
    if (HdfsIO.exists(outPath + "/" + MimeTypeCountFile)) Some {
      if (HdfsIO.files(outPath + "/_*-" + targetFile).isEmpty) ProcessingState.Finished
      else ProcessingState.Failed
    } else None

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] =
    TextTypes.keys.iterator.map(
      p =>
        DerivativeOutput(
          p + "-" + targetFile,
          conf.outputPath + relativeOutPath,
          "csv",
          "application/gzip"))

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] =
    super.templateVariables(conf) ++ Seq("showPreview" -> false)
}

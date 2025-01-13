package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.matchbox.GetExtensionMIME
import org.apache.commons.io.FilenameUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.http.HttpMessage
import org.archive.webservices.sparkling.io.InputStreamForker
import org.archive.webservices.sparkling.util.{Common, DigestUtil}
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.{InputStream, PrintStream}
import java.net.URL
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

object ImageInformationExtraction extends BinaryInformationAutJob {
  val name = "Image file information"
  val uuid = "01895067-d598-7db8-88ad-46fed66e27f5"

  override val infoUrl =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/14410815476500-ARCH-File-format-datasets#image"

  val description =
    "Locations and metadata for JPEG, PNG, GIF, and other image formatted files in the collection. Output: one CSV with columns for crawl date, last modified date, URL, file name, file format extension, MIME type as reported by the web server and as detected by Apache TIKA, and MD5 and SHA1 hash values."

  val targetFile: String = "image-information.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date, last_modified_date, url, filename, extension, mime_type_web_server, mime_type_tika, width, height, md5, sha1")

  override def checkMime(url: String, server: String, tika: String): Boolean =
    tika.startsWith("image/")

  override def df(rdd: RDD[Row]): Dataset[Row] = AutLoader.images(rdd)

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
        val Array(imageIn, md5In, sha1In) = forker.fork(3).map(Future(_))
        val Seq((width: Int, height: Int), md5: String, sha1: String) =
          try {
            Await.result(
              Future.sequence(
                Seq(
                  imageIn.map(in => Common.cleanup(AutUtil.computeImageSize(in))(in.close)),
                  md5In.map(DigestUtil.md5Hex),
                  sha1In.map(DigestUtil.sha1Hex))),
              Duration.Inf)
          } finally {
            for (s <- imageIn) Try(s.close())
            for (s <- md5In) Try(s.close())
            for (s <- sha1In) Try(s.close())
            Try(body.close())
          }

        val jUrl = new URL(url)
        val filename = FilenameUtils.getName(jUrl.getPath)
        val extension = GetExtensionMIME(jUrl.getPath, tikaMime)
        val lastModifiedDate =
          AutUtil.rfc1123toTime14(http.headerMap.get("last-modified").getOrElse(""))

        Row(
          crawlDate,
          lastModifiedDate,
          url,
          filename,
          extension,
          AutUtil.mime(http),
          tikaMime,
          width,
          height,
          md5,
          sha1)
      })

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

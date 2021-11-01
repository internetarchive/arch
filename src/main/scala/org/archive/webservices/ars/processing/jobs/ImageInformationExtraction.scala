package org.archive.webservices.ars.processing.jobs

import java.io.{InputStream, PrintStream}
import java.net.URL

import io.archivesunleashed.matchbox.GetExtensionMIME
import org.apache.commons.io.FilenameUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.helge.sparkling.http.HttpMessage
import org.archive.helge.sparkling.io.InputStreamForker
import org.archive.helge.sparkling.util.{Common, DigestUtil}
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.helge.sparkling.Sparkling.executionContext

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

object ImageInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract image information"

  val description =
    "Create a CSV with the following columns: crawl date, URL of the image, filename, image extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, image width, image height, image MD5 hash and image SHA1 hash."

  val targetFile: String = "image-information.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date,url,filename,extension,mime_type_web_server,mime_type_tika,width,height,md5,sha1")

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
          crawlDate: String) => {
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

        Row(
          crawlDate,
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

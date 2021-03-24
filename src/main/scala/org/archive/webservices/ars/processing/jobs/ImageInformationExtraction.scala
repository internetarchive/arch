package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.ImageInformationExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob

object ImageInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract image information"
  val description =
    "Create a CSV with the following columns: crawl date, URL of the image, filename, image extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, image width, image height, image MD5 hash and image SHA1 hash."

  val targetFile: String = "image-information.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date, url, filename, extension, mime_type_web_server, mime_type_tika, width, height, md5, sha1")

  def df(rdd: RDD[ArchiveRecord]) = ImageInformationExtractor(rdd.images())
}

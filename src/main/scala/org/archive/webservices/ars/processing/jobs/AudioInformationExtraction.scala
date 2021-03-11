package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.AudioInformationExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object AudioInformationExtraction extends AutJob {
  val name = "Extract audio information"
  val category = ArsCloudJobCategories.BinaryInformation
  val description =
    "Create a CSV with the following columns: crawl date, URL of the audio file, filename, audio extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, audio MD5 hash and audio SHA1 hash."

  val targetFile: String = "audio-information.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date, url, filename, extension, mime_type_web_server, mime_type_tika, md5, sha1")

  def df(rdd: RDD[ArchiveRecord]) = AudioInformationExtractor(rdd.audio())
}

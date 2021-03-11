package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.WordProcessorInformationExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object WordProcessorInformationExtraction extends AutJob {
  val name = "Extract word processor information"
  val category = ArsCloudJobCategories.BinaryInformation
  val description =
    "Create a CSV with the following columns: crawl date, URL of the word processor program file, filename, word processor program extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, word processor program MD5 hash and word processor program SHA1 hash."

  val targetFile: String = "word-processor-information.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date, url, filename, extension, mime_type_web_server, mime_type_tika, md5, sha1")

  def df(rdd: RDD[ArchiveRecord]) = WordProcessorInformationExtractor(rdd.wordProcessorFiles())
}

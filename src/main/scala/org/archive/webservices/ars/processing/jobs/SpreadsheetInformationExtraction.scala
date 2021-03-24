package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.SpreadsheetInformationExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob

object SpreadsheetInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract spreadsheet information"

  val description =
    "Create a CSV with the following columns: crawl date, URL of the spreadsheet file, filename, spreadsheet extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, spreadsheet MD5 hash and spreadsheet SHA1 hash."

  val targetFile: String = "spreadsheet-information.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date, url, filename, extension, mime_type_web_server, mime_type_tika, md5, sha1")

  def df(rdd: RDD[ArchiveRecord]) = SpreadsheetInformationExtractor(rdd.spreadsheets())
}

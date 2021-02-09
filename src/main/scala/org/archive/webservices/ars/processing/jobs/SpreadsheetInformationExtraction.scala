package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.SpreadsheetInformationExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object SpreadsheetInformationExtraction extends AutJob {
  val name = "Extract spreadsheet information"
  val category = ArsCloudJobCategories.BinaryInformation
  val description =
    "This will output a single file with the following columns: crawl date, URL of the spreadsheet file, filename, spreadsheet extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, spreadsheet MD5 hash and spreadsheet SHA1 hash."

  val targetFile: String = "spreadsheet-information.csv.gz"

  def df(rdd: RDD[ArchiveRecord]) = SpreadsheetInformationExtractor(rdd.spreadsheets())
}

package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob

object SpreadsheetInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract spreadsheet information"

  val description =
    "Create a CSV with the following columns: crawl date, URL of the spreadsheet file, filename, spreadsheet extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, spreadsheet MD5 hash and spreadsheet SHA1 hash."

  val targetFile: String = "spreadsheet-information.csv.gz"

  val SpreadsheetMimeTypes: Set[String] = Set(
    "application/vnd.ms-excel",
    "application/vnd.ms-excel.workspace.3",
    "application/vnd.ms-excel.workspace.4",
    "application/vnd.ms-excel.sheet.2",
    "application/vnd.ms-excel.sheet.3",
    "application/vnd.ms-excel.sheet.3",
    "application/vnd.ms-excel.addin.macroenabled.12",
    "application/vnd.ms-excel.sheet.binary.macroenabled.12",
    "application/vnd.ms-excel.sheet.macroenabled.12",
    "application/vnd.ms-excel.template.macroenabled.12",
    "application/vnd.ms-spreadsheetml",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/x-vnd.oasis.opendocument.spreadsheet-template",
    "application/vnd.oasis.opendocument.spreadsheet-template",
    "application/vnd.oasis.opendocument.spreadsheet",
    "application/x-vnd.oasis.opendocument.spreadsheet",
    "application/x-tika-msworks-spreadsheet",
    "application/vnd.lotus-1-2-3",
    "text/csv",
    "text/tab-separated-values")

  override def checkMime(url: String, server: String, tika: String): Boolean =
    SpreadsheetMimeTypes.contains(tika) || server == "text/csv" || server == "text/tab-separated-values" || ((url.toLowerCase
      .endsWith(".csv") || url.toLowerCase.endsWith(".tsv")) && tika == "text/plain")

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

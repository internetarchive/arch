package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object SpreadsheetInformationExtraction extends BinaryInformationAutJob {
  val name = "Spreadsheet file information"
  val uuid = "01895069-192a-74f8-84a9-b14f20c20f89"

  val description =
    "Locations and metadata for CSV, XLS, ODS, and other spreadsheet formatted files in the collection. Output: one CSV with columns for crawl date, last modified date, URL, file name, file format extension, MIME type as reported by the web server and as detected by Apache TIKA, and MD5 and SHA1 hash values."

  val targetFile: String = "spreadsheet-information.csv.gz"

  val SpreadsheetMimeTypes: Set[String] = Set(
    " application/vnd.apple.numbers",
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

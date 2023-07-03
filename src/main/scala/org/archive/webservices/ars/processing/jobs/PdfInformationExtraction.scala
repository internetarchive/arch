package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object PdfInformationExtraction extends BinaryInformationAutJob {
  val name = "PDF file information"

  val description =
    "Locations and metadata for Portable Document Format (PDF) files in the collection. Output: one CSV with columns for crawl date, last modified date, URL, file name, file format extension, MIME type as reported by the web server and as detected by Apache TIKA, and MD5 and SHA1 hash values."

  val targetFile: String = "pdf-information.csv.gz"

  override def checkMime(url: String, server: String, tika: String): Boolean =
    tika == "application/pdf"

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

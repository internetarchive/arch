package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object PdfInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract PDF information"

  val description =
    "Create a CSV with the following columns: crawl date, last modified date, URL of the PDF file, filename, PDF extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, PDF MD5 hash and PDF SHA1 hash."

  val targetFile: String = "pdf-information.csv.gz"

  override def checkMime(url: String, server: String, tika: String): Boolean =
    tika == "application/pdf"

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

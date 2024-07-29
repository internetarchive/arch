package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object PdfInformationExtraction extends BinaryInformationAutJob {
  val name = "PDF file information"
  val uuid = "01895068-3e02-72cb-b0d9-4e1bacc42c37"

  override val infoUrl = "https://arch-webservices.zendesk.com/hc/en-us/articles/14410815476500-ARCH-File-format-datasets#pdf"

  val description =
    "Locations and metadata for Portable Document Format (PDF) files in the collection. Output: one CSV with columns for crawl date, last modified date, URL, file name, file format extension, MIME type as reported by the web server and as detected by Apache TIKA, and MD5 and SHA1 hash values."

  val targetFile: String = "pdf-information.csv.gz"

  override def checkMime(url: String, server: String, tika: String): Boolean =
    server == "application/pdf" // not `tika == `, which we had before, but also matches Adobe Illustrator and PostScript

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

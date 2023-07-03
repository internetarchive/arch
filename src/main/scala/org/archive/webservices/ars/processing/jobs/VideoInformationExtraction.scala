package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object VideoInformationExtraction extends BinaryInformationAutJob {
  val name = "Video file information"

  val description =
    "Locations and metadata for MP4, MOV, AVI, and other video formatted files in the collection. Output: one CSV with columns for crawl date, last modified date, URL, file name, file format extension, MIME type as reported by the web server and as detected by Apache TIKA, and MD5 and SHA1 hash values."

  val targetFile: String = "video-information.csv.gz"

  override def checkMime(url: String, server: String, tika: String): Boolean =
    tika.startsWith("video/")

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

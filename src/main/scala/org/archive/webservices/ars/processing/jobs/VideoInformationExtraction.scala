package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob

object VideoInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract video information"

  val description =
    "Create a CSV with the following columns: crawl date, URL of the video file, filename, video extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, video MD5 hash and video SHA1 hash."

  val targetFile: String = "video-information.csv.gz"

  override def checkMime(url: String, server: String, tika: String): Boolean =
    tika.startsWith("video/")

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

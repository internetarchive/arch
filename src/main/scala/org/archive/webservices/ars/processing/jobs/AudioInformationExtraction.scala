package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object AudioInformationExtraction extends BinaryInformationAutJob {
  val name = "Audio file information"

  val description =
    "Create a CSV with the following columns: crawl date, last modified date, URL of the audio file, filename, audio extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, audio MD5 hash and audio SHA1 hash."

  val targetFile: String = "audio-information.csv.gz"

  def checkMime(url: String, server: String, tika: String): Boolean =
    tika.startsWith("audio/")

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

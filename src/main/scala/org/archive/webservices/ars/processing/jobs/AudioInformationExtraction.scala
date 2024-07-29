package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object AudioInformationExtraction extends BinaryInformationAutJob {
  val name = "Audio file information"
  val uuid = "01895066-7db2-794b-b91b-e3f5a340e859"

  override val infoUrl = "https://arch-webservices.zendesk.com/hc/en-us/articles/14410815476500-ARCH-File-format-datasets#audio"

  val description =
    "Locations and metadata for MP3, WAV, AAC, and other audio formatted files in the collection. Output: one CSV with columns for crawl date, last modified date, URL, file name, file format extension, MIME type as reported by the web server and as detected by Apache TIKA, and MD5 and SHA1 hash values."

  val targetFile: String = "audio-information.csv.gz"

  def checkMime(url: String, server: String, tika: String): Boolean =
    tika.startsWith("audio/")

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

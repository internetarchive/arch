package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object PresentationProgramInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract PowerPoint (e.g., .ppt, .odp, .key) information"

  val description =
    "Create a CSV with the following columns: crawl date, last modified date, URL of a PowerPoint or similar file, filename, PowerPoint or similar file extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, PowerPoint or similar file MD5 hash and PowerPoint or similar file SHA1 hash."

  val targetFile: String = "powerpoint-information.csv.gz"

  val PresentationMimeTypes: Set[String] = Set(
    "application/vnd.apple.keynote",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.oasis.opendocument.presentation",
    "application/vnd.oasis.opendocument.presentation-template",
    "application/vnd.sun.xml.impress",
    "application/vnd.sun.xml.impress.template",
    "application/vnd.stardivision.impress",
    "application/x-starimpress",
    "application/vnd.ms-powerpoint.addin.macroEnabled.12",
    "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
    "application/vnd.ms-powerpoint.slide.macroEnabled.12",
    "application/vnd.ms-powerpoint.slideshow.macroEnabled.12",
    "application/vnd.ms-powerpoint.template.macroEnabled.12")

  override def checkMime(url: String, server: String, tika: String): Boolean =
    PresentationMimeTypes.contains(tika)

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

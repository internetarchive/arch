package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object PresentationProgramInformationExtraction extends BinaryInformationAutJob {
  val name = "Presentation file information"

  val description =
    "Locations and metadata for PowerPoint, Keynote, and other presentation formatted files in the collection. Output: one CSV with columns for crawl date, last modified date, URL, file name, file format extension, MIME type as reported by the web server and as detected by Apache TIKA, and MD5 and SHA1 hash values."

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

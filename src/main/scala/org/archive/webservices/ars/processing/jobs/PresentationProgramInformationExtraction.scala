package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob

object PresentationProgramInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract presentation program information"

  val description =
    "Create a CSV with the following columns: crawl date, URL of the presentation program file, filename, presentation program extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, presentation program MD5 hash and presentation program SHA1 hash."

  val targetFile: String = "presentation-program-information.csv.gz"

  val PresentationMimeTypes: Set[String] = Set(
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

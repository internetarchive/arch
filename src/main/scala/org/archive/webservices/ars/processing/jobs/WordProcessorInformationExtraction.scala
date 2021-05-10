package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob

object WordProcessorInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract word processor information"

  val description =
    "Create a CSV with the following columns: crawl date, URL of the word processor program file, filename, word processor program extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, word processor program MD5 hash and word processor program SHA1 hash."

  val targetFile: String = "word-processor-information.csv.gz"

  val WordProcessorMimeTypes: Set[String] = Set(
    "application/vnd.lotus-wordpro",
    "application/vnd.kde.kword",
    "application/vnd.ms-word.document.macroEnabled.12",
    "application/vnd.ms-word.template.macroEnabled.12",
    "application/vnd.oasis.opendocument.text",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document.glossary+xml",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml",
    "application/vnd.wordperfect",
    "application/wordperfect5.1",
    "application/msword",
    "application/vnd.ms-word.document.macroEnabled.12",
    "application/vnd.ms-word.template.macroEnabled.12",
    "application/vnd.apple.pages",
    "application/macwriteii",
    "application/vnd.ms-works",
    "application/rtf")

  override def checkMime(url: String, server: String, tika: String): Boolean =
    WordProcessorMimeTypes.contains(tika)

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = rdd.flatMap(prepareRecord)
}

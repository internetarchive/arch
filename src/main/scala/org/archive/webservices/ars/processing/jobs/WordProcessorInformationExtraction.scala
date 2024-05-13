package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.sparkling.warc.WarcRecord

object WordProcessorInformationExtraction extends BinaryInformationAutJob {
  val name = "Word processing file information"
  val uuid = "0189506a-d09d-7571-9d3c-a44698d58d39"

  override val infoUrl = "https://arch-webservices.zendesk.com/hc/en-us/articles/14410815476500-ARCH-File-format-datasets#word"

  val description =
    "Locations and metadata for DOC, RTF, ODT, and other word processing files in the collection. Output: one CSV with columns for crawl date, last modified date, URL, file name, file format extension, MIME type as reported by the web server and as detected by Apache TIKA, and MD5 and SHA1 hash values."

  val targetFile: String = "word-document-information.csv.gz"

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

package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.matchbox.{DetectLanguage, RemoveHTML}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.ars.util.{HttpUtil, PublicSuffixUtil}
import org.archive.webservices.sparkling.http.HttpMessage
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.{InputStream, PrintStream}

object WebPagesExtraction extends BinaryInformationAutJob {
  val name = "Plain text of webpages"
  val uuid = "0189506a-46f3-7d73-9dcf-a8fce59c50cc"

  override val category: ArchJobCategory = ArchJobCategories.Text

  val description =
    "Location, technical metadata, and extracted full text contents of each text-bearing document in the collection. Output: one CSV file with columns for crawl date, last modified date, domain, URL, MIME type as reported by the web server and as detected by Apache TIKA, and content."

  val targetFile: String = "web-pages.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date,last_modified_date,domain,url,mime_type_web_server,mime_type_tika,language,content")

  override def checkMime(url: String, server: String, tika: String): Boolean =
    AutUtil.checkPageMime(url, server)

  override def df(rdd: RDD[Row]): Dataset[Row] = AutLoader.webpages(rdd)

  override def prepareRecord(r: WarcRecord): Option[Row] =
    throw new RuntimeException(
      "This method should not be called in WebPagesExtraction, see #prepareRecords")

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = {
    val publicSuffixes = PublicSuffixUtil.broadcast(rdd.context)
    rdd.flatMap { r =>
      prepareBinaryRow(
        r,
        (
            url: String,
            http: HttpMessage,
            body: InputStream,
            tikaMime: String,
            crawlDate: String,
            lastModifiedDate: String) => {
          val bodyString = HttpUtil.bodyString(body, http)
          val content = RemoveHTML(bodyString)
          Row(
            crawlDate,
            lastModifiedDate,
            AutUtil.extractDomainRemovePrefixWWW(url, publicSuffixes.value),
            url,
            AutUtil.mime(http),
            tikaMime,
            DetectLanguage(content),
            content)
        })
    }
  }

  override val templateName: Option[String] = Some("jobs/DefaultAutJob")
}

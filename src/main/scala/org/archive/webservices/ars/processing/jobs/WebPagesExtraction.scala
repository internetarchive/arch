package org.archive.webservices.ars.processing.jobs

import java.io.{InputStream, PrintStream}

import io.archivesunleashed.matchbox.{DetectLanguage, RemoveHTML, RemoveHTTPHeader}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.helge.sparkling.http.HttpMessage
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob
import org.archive.webservices.ars.util.{HttpUtil, PublicSuffixUtil}

object WebPagesExtraction extends BinaryInformationAutJob {
  val name = "Extract plain text of webpages"

  override val category: ArchJobCategory = ArchJobCategories.Text

  val description =
    "Create a CSV with the following columns: crawl date, web domain, URL, MIME type as provided by the web server, MIME type as detected by Apache TIKA, and content (HTTP headers and HTML removed)."

  val targetFile: String = "web-pages.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println("crawl_date,domain,url,mime_type_web_server,mime_type_tika,language,content")

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
            crawlDate: String) => {
          val bodyString = HttpUtil.bodyString(body, http)
          val content = RemoveHTML(RemoveHTTPHeader(bodyString))
          Row(
            crawlDate,
            AutUtil.extractDomainRemovePrefixWWW(url, publicSuffixes.value),
            url,
            AutUtil.mime(http),
            tikaMime,
            DetectLanguage(content),
            content)
        })
    }
  }

  override def templateName: Option[String] = Some("jobs/WebPagesExtraction")
}

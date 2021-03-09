package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.WebPagesExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object WebPagesExtraction extends AutJob {
  val name = "Extract webpages"
  val category = ArsCloudJobCategories.Text
  val description =
    "Create a CSV with the following columns: crawl date, web domain, URL, MIME type as provided by the web server, MIME type as detected by Apache TIKA, and content (HTTP headers and HTML removed)."

  val targetFile: String = "web-pages.csv.gz"

  def df(rdd: RDD[ArchiveRecord]) = WebPagesExtractor(rdd.webpages())
}

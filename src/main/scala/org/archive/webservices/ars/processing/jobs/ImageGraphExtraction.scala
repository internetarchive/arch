package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.matchbox.ExtractImageLinks
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.processing.jobs.shared.NetworkAutJob
import org.archive.webservices.ars.util.{Common, HttpUtil}
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.PrintStream

object ImageGraphExtraction extends NetworkAutJob[Row] {
  val name = "Extract image graph"
  val description =
    "Create a CSV with the following columns: crawl date, source of the image (where it was hosted), the URL of the image, and the alternative text of the image."

  val targetFile: String = "image-graph.csv.gz"

  val srcDstFields: (String, String) = ("src", "image_url")

  override def printToOutputStream(out: PrintStream): Unit =
    out.println("crawl_date,source,url,alt_text")

  override def df(rdd: RDD[Row]): Dataset[Row] = AutLoader.imageGraph(rdd)

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = {
    rdd.flatMap { r =>
      r.http.filter(AutUtil.validPage(r, _)).toIterator.flatMap { http =>
        Common
          .tryOrElse(Seq.empty[Row]) {
            val url = AutUtil.url(r)
            AutUtil
              .extractLinks(ExtractImageLinks.apply, url, HttpUtil.bodyString(http.body, http))
              .map {
                case (source, target, alt) =>
                  Row(AutUtil.timestamp(r), source, target, alt)
              }
          }
          .toIterator
      }
    }
  }
}

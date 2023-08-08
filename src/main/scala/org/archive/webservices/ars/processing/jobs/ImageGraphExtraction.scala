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
  val name = "Image graph"
  val uuid = "01895067-92fb-739c-a99d-037fde1798a4"
  val description =
    "Timestamp, location, and any original description for each image file in the collection. Output: one CSV with columns for crawl date, source page, image file url, and alt text."

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

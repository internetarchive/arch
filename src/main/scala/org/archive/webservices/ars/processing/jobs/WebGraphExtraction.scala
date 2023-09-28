package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.matchbox.ExtractLinks
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.processing.jobs.shared.NetworkAutJob
import org.archive.webservices.ars.util.{Common, HttpUtil}
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.PrintStream

object WebGraphExtraction extends NetworkAutJob[Row] {
  val name = "Web graph"
  val uuid = "01895069-e74c-79de-8292-effb45265179"
  val description =
    "Links between all documents in the collection over time and any descriptive anchor text about with them. Output: one CSV file with columns for crawl date, source, target, and anchor text."

  val targetFile: String = "web-graph.csv.gz"

  val srcDstFields: (String, String) = ("src", "dest")

  override def printToOutputStream(out: PrintStream): Unit =
    out.println("crawl_date,source,target,anchor_text")

  override def df(rdd: RDD[Row]): Dataset[Row] = AutLoader.webGraph(rdd)

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[Row] = {
    rdd.flatMap { r =>
      r.http.filter(AutUtil.validPage(r, _)).toIterator.flatMap { http =>
        Common
          .tryOrElse(Seq.empty[Row]) {
            val url = AutUtil.url(r)
            AutUtil
              .extractLinks(ExtractLinks.apply, url, HttpUtil.bodyString(http.body, http))
              .map { case (source, target, alt) =>
                Row(AutUtil.timestamp(r), source, target, alt)
              }
          }
          .toIterator
      }
    }
  }
}

package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.matchbox.ExtractLinks
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.webservices.sparkling.warc.WarcRecord
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.processing.jobs.shared.NetworkAutJob
import org.archive.webservices.ars.util.{Common, HttpUtil}

object WebGraphExtraction extends NetworkAutJob[Row] {
  val name = "Extract web graph"
  val description =
    "Create a CSV with the following columns: crawl date, source, target, and anchor text. Note that this contains all links and is not aggregated into domains."

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

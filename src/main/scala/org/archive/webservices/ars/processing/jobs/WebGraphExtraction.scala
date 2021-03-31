package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.WebGraphExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.processing.jobs.shared.NetworkAutJob

object WebGraphExtraction extends NetworkAutJob {
  val name = "Extract web graph"
  val description =
    "Create a CSV with the following columns: crawl date, source, destination, and anchor text. Note that this contains all links and is not aggregated into domains."

  val targetFile: String = "web-graph.csv.gz"

  val srcDstFields: (String, String) = ("src", "dest")

  override def printToOutputStream(out: PrintStream): Unit =
    out.println("crawl_date, source, destination, anchor_text")

  def df(rdd: RDD[ArchiveRecord]) = WebGraphExtractor(rdd.webgraph())
}

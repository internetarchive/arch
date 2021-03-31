package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.DomainGraphExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.processing.jobs.shared.NetworkAutJob

object DomainGraphExtraction extends NetworkAutJob {
  val name = "Extract domain graph"
  val description =
    "Create a CSV with the following columns: crawl date, source domain, destination domain, and count."

  val targetFile: String = "domain-graph.csv.gz"

  val srcDstFields: (String, String) = ("src_domain", "dest_domain")

  override def printToOutputStream(out: PrintStream): Unit =
    out.println("crawl_date, source, destination, count")

  def df(rdd: RDD[ArchiveRecord]) = DomainGraphExtractor(rdd.webgraph())
}

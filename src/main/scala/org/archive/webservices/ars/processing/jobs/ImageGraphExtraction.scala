package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.ImageGraphExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.processing.jobs.shared.NetworkAutJob

object ImageGraphExtraction extends NetworkAutJob {
  val name = "Extract image graph"
  val description =
    "Create a CSV with the following columns: crawl date, source of the image (where it was hosted), the URL of the image, and the alternative text of the image."

  val targetFile: String = "image-graph.csv.gz"

  val srcDstFields: (String, String) = ("src", "image_url")

  override def printToOutputStream(out: PrintStream): Unit =
    out.println("crawl_date, source, url, alt_text")

  def df(rdd: RDD[ArchiveRecord]) = ImageGraphExtractor(rdd.imagegraph())
}

package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.WebGraphExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object WebGraphExtraction extends AutJob {
  val name = "Extract web graph"
  val category = ArsCloudJobCategories.Network
  val description =
    "This will output a single file with the following columns: crawl date, source, destination, and anchor text. Note that this contains all links and is not aggregated into domains."

  val targetFile: String = "web-graph.csv.gz"

  def df(rdd: RDD[ArchiveRecord]) = WebGraphExtractor(rdd.webgraph())
}

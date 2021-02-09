package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.DomainGraphExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object DomainGraphExtraction extends AutJob {
  val name = "Extract domain graph"
  val category = ArsCloudJobCategories.Network
  val description =
    "This will output a single file with the following columns crawl date, source domain, destination domain, and count."
  val targetFile: String = "domain-graph.csv.gz"

  def df(rdd: RDD[ArchiveRecord]) = DomainGraphExtractor(rdd.webgraph())
}

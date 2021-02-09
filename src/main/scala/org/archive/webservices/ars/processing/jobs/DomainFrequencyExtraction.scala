package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.DomainFrequencyExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object DomainFrequencyExtraction extends AutJob {
  val name = "Domain Frequency"
  val category = ArsCloudJobCategories.Collection
  val description =
    "This will output a single file with the following two columns: domain and count."

  val targetFile: String = "domain-frequency.csv.gz"

  def df(rdd: RDD[ArchiveRecord]) = DomainFrequencyExtractor(rdd.webpages())
}

package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.DomainFrequencyExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object DomainFrequencyExtraction extends AutJob {
  val name = "Domain Frequency"
  val category = ArsCloudJobCategories.Collection
  val description =
    "Create a CSV with the following columns: domain and count."

  val targetFile: String = "domain-frequency.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit = out.println("domain, count")

  def df(rdd: RDD[ArchiveRecord]) = DomainFrequencyExtractor(rdd.webpages())
}

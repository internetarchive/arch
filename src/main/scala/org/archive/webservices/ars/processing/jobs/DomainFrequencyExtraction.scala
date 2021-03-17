package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.DomainFrequencyExtractor
import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object DomainFrequencyExtraction extends AutJob {
  val name = "Domain Frequency"
  val category = ArsCloudJobCategories.Collection
  val description =
    "Create a CSV with the following columns: domain and count."

  val targetFile: String = "domain-frequency.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit = out.println("domain, count")

  def df(rdd: RDD[ArchiveRecord]) = DomainFrequencyExtractor(rdd.webpages())

  override def templateName: Option[String] = Some("jobs/DomainFrequencyExtraction")

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    val topDomains = HdfsIO
      .lines(conf.outputPath + relativeOutPath + "/" + targetFile, 11)
      .drop(1)
      .map(_.split(','))
      .map {
        case Array(domain, freq) =>
          (domain, freq.toInt)
      }
    super.templateVariables(conf) ++ Seq("topDomains" -> topDomains)
  }
}

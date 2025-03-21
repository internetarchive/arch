package org.archive.webservices.ars.processing.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.desc
import org.apache.spark.sql.{Dataset, Row}
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.jobs.shared.AutJob
import org.archive.webservices.ars.processing.{DerivationJobConf, ProcessingState, SampleVizData}
import org.archive.webservices.ars.util.{Common, PublicSuffixUtil}
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.PrintStream

object DomainFrequencyExtraction extends AutJob[(String, Long)] {
  val name = "Domain frequency"
  val uuid = "01894bc7-ff6a-7e25-a5b5-4570425a8ab7"
  val category: ArchJobCategory = ArchJobCategories.Collection

  override val infoUrl =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/14410734896148-ARCH-Collection-datasets#domain-frequency"

  val description =
    "The number of unique documents collected from each domain in the collection. Output: one CSV file with columns for domain and count."

  val targetFile: String = "domain-frequency.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit = out.println("domain, count")

  override def df(rdd: RDD[(String, Long)]): Dataset[Row] = {
    val rows = rdd
      .reduceByKey(_ + _)
      .map { case (domain, count) =>
        Row(domain, count)
      }
    AutLoader.domainFrequency(rows).orderBy(desc("count"))
  }

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[(String, Long)] = {
    val publicSuffixes = PublicSuffixUtil.broadcast(rdd.context)
    rdd
      .flatMap { r =>
        Common.tryOrElse[Option[(String, Long)]](None) {
          r.http.filter(AutUtil.validPage(r, _)).map { _ =>
            val url = AutUtil.url(r)
            (AutUtil.extractDomainRemovePrefixWWW(url, publicSuffixes.value), 1L)
          }
        }
      }
  }

  override val templateName: Option[String] = Some("jobs/DomainFrequencyExtraction")

  override def sampleVizData(conf: DerivationJobConf): Option[SampleVizData] =
    checkFinishedState(conf.outputPath + relativeOutPath) match {
      case Some(ProcessingState.Finished) =>
        Some(
          SampleVizData(
            HdfsIO
              .lines(conf.outputPath + relativeOutPath + "/" + targetFile, 11)
              .drop(1)
              .flatMap { line =>
                val comma = line.lastIndexOf(',')
                if (comma < 0) None
                else
                  Some {
                    val (domain, freq) =
                      (line.take(comma).stripPrefix("\"").stripSuffix("\""), line.drop(comma + 1))
                    (domain, freq)
                  }
              }))
      case _ => None
    }

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    super.templateVariables(conf) ++ Seq(
      "topDomains" ->
        sampleVizData(conf).map(_.nodes).getOrElse(Seq.empty))
  }
}

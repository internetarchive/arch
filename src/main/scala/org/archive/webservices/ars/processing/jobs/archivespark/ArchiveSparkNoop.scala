package org.archive.webservices.ars.processing.jobs.archivespark

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkBaseJob}

object ArchiveSparkNoop extends ArchiveSparkBaseJob {
  val name: String = id
  val uuid: String = "018d1cef-c91d-7d51-9cf4-05fe51900321"
  val description: String =
    "Am ArchiveSpark job that does nothing. Output: records turned into ArchiveSpark JSON format without any enrichment function applied."
  val category: ArchJobCategory = ArchJobCategories.None

  override def enrich(
      rdd: RDD[ArchEnrichRoot[_]],
      conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = rdd
}

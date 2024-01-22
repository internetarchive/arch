package org.archive.webservices.ars.processing.jobs.archivespark

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.dataspecs.DataSpec
import org.archive.webservices.ars.model.collections.inputspecs.FileRecord
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchFileSpec, ArchWarcSpec, ArchiveSparkBaseJob}
import org.archive.webservices.sparkling.warc.WarcRecord

object ArchiveSparkNoop extends ArchiveSparkBaseJob[ArchEnrichRoot[_]] {
  val name: String = id
  val uuid: String = "018d1cef-c91d-7d51-9cf4-05fe51900321"
  val description: String = "ArchiveSpark job " + name
  val category: ArchJobCategory = ArchJobCategories.None

  override def warcSpec(rdd: RDD[WarcRecord]): DataSpec[_, ArchEnrichRoot[_]] = ArchWarcSpec(rdd)
  override def fileSpec(rdd: RDD[FileRecord]): DataSpec[_, ArchEnrichRoot[_]] = ArchFileSpec(rdd)

  override def filterEnrich(rdd: RDD[ArchEnrichRoot[_]], conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = rdd
}
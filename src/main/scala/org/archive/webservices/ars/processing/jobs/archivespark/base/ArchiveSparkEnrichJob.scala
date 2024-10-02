package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.archivespark.model.dataloads.ByteLoad
import org.archive.webservices.archivespark.model.pointers.DataLoadPointer
import org.archive.webservices.ars.processing.DerivationJobConf

abstract class ArchiveSparkEnrichJob extends ArchiveSparkBaseJob {
  def byteLoad: DataLoadPointer[ArchEnrichRoot[_], Array[Byte]] =
    ArchiveSparkEnrichJob.byteLoad

  def fileLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    ArchiveSparkEnrichJob.fileLoad

  def plainTextLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    ArchiveSparkEnrichJob.plainTextLoad

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]]

  override def enrich(
      rdd: RDD[ArchEnrichRoot[_]],
      conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = {
    var enriched = rdd
    for (func <- functions(conf)) enriched = enriched.enrich(func)
    enriched.map { r =>
      r.clearCache()
      r
    }
  }
}

object ArchiveSparkEnrichJob {
  val byteLoad: DataLoadPointer[ArchEnrichRoot[_], Array[Byte]] =
    DataLoadPointer(ByteLoad)

  val fileLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    DataLoadPointer(FileLoad)

  val plainTextLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    DataLoadPointer(PlainTextLoad)
}

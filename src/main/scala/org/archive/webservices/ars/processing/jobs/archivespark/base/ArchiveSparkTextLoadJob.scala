package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.dataspecs.DataSpec
import org.archive.webservices.archivespark.model.pointers.DataLoadPointer
import org.archive.webservices.ars.model.collections.inputspecs.FileRecord
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.sparkling.warc.WarcRecord

abstract class ArchiveSparkTextLoadJob extends ArchiveSparkEnrichJob[ArchEnrichRoot[_] with PlainTextLoad.Root] {
  override def filter(rdd: RDD[ArchEnrichRoot[_] with PlainTextLoad.Root], conf: DerivationJobConf): RDD[ArchEnrichRoot[_] with PlainTextLoad.Root] = {
    rdd.filter(_.mime.startsWith("text/"))
  }

  val textLoad: DataLoadPointer[ArchEnrichRoot[_] with PlainTextLoad.Root, String] = DataLoadPointer(PlainTextLoad)

  def warcSpec(rdd: RDD[WarcRecord]): DataSpec[_, ArchEnrichRoot[_] with PlainTextLoad.Root] = ArchWarcSpec(rdd)
  def fileSpec(rdd: RDD[FileRecord]): DataSpec[_, ArchEnrichRoot[_] with PlainTextLoad.Root] = ArchFileSpec(rdd)
}
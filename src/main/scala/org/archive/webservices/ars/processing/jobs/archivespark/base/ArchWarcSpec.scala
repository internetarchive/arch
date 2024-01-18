package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.dataspecs.DataSpec
import org.archive.webservices.sparkling.warc.WarcRecord

class ArchWarcSpec(@transient val rdd: RDD[WarcRecord]) extends DataSpec[WarcRecord, ArchWarcRecord] {
  override def load(sc: SparkContext, minPartitions: Int): RDD[WarcRecord] = rdd
  override def parse(warc: WarcRecord): Option[ArchWarcRecord] = Some(new ArchWarcRecord(warc))
}

object ArchWarcSpec {
  def apply(rdd: RDD[WarcRecord]) = new ArchWarcSpec(rdd)
}

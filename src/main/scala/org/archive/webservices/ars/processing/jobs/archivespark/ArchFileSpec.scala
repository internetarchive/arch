package org.archive.webservices.ars.processing.jobs.archivespark

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.dataspecs.DataSpec
import org.archive.webservices.ars.model.collections.filespecs.FileRecord
import org.archive.webservices.sparkling.warc.WarcRecord

class ArchFileSpec(@transient val rdd: RDD[FileRecord]) extends DataSpec[FileRecord, ArchFileRecord] {
  override def load(sc: SparkContext, minPartitions: Int): RDD[FileRecord] = rdd
  override def parse(file: FileRecord): Option[ArchFileRecord] = Some(new ArchFileRecord(file))
}

object ArchFileSpec {
  def apply(rdd: RDD[FileRecord]) = new ArchFileSpec(rdd)
}

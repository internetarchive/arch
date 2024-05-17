package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.dataspecs.DataSpec
import org.archive.webservices.archivespark.model.dataloads.ByteLoad
import org.archive.webservices.archivespark.model.pointers.DataLoadPointer
import org.archive.webservices.ars.model.collections.inputspecs.FileRecord
import org.archive.webservices.sparkling.warc.WarcRecord

abstract class ArchiveSparkArchJob
    extends ArchiveSparkEnrichJob[ArchEnrichRoot[_]] {
  def byteLoad: DataLoadPointer[ArchEnrichRoot[_], Array[Byte]] =
    ArchiveSparkArchJob.byteLoad

  def fileLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    ArchiveSparkArchJob.fileLoad

  def plainTextLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    ArchiveSparkArchJob.plainTextLoad

  def warcSpec(rdd: RDD[WarcRecord]): DataSpec[_, ArchEnrichRoot[_]] =
    ArchWarcSpec(rdd)

  def fileSpec(rdd: RDD[FileRecord]): DataSpec[_, ArchEnrichRoot[_]] =
    ArchFileSpec(rdd)
}

object ArchiveSparkArchJob {
  val byteLoad: DataLoadPointer[ArchEnrichRoot[_], Array[Byte]] =
    DataLoadPointer(ByteLoad)

  val fileLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    DataLoadPointer(FileLoad)

  val plainTextLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    DataLoadPointer(PlainTextLoad)
}
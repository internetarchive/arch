package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.archivespark.model.dataloads.ByteLoad
import org.archive.webservices.archivespark.model.pointers.DataLoadPointer
import org.archive.webservices.archivespark.util.Bytes
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.StageSyncManager
import org.archive.webservices.sparkling.util.IteratorUtil

abstract class ArchiveSparkEnrichJob extends ArchiveSparkBaseJob {
  def byteLoad: DataLoadPointer[ArchEnrichRoot[_], Bytes] =
    ArchiveSparkEnrichJob.byteLoad

  def fileLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    ArchiveSparkEnrichJob.fileLoad

  def plainTextLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    ArchiveSparkEnrichJob.plainTextLoad

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]]

  override def enrich(
      rdd: RDD[ArchEnrichRoot[_]],
      conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = {
    val funcs = functions(conf)
    var enriched = if (funcs.length <= 1) rdd else {
      val (longest, longestPath) = funcs.map(f => (f, f.dependencyPath.toSet)).maxBy(_._2.size)
      if (funcs.exists(f => f != longest && !longestPath.contains(f))) rdd.map { r =>
        r.cacheEnabled = true
        r
      } else rdd
    }
    for (func <- funcs) enriched = enriched.enrich(func)
    enriched.mapPartitions { partition =>
      StageSyncManager.lockMutex(blocking = Sparkling.taskContext.exists(_.attemptNumber > 0))
      partition.map { r =>
        r.clearCache()
        r
      } ++ IteratorUtil.noop {
        StageSyncManager.unlockMutex()
      }
    }
  }
}

object ArchiveSparkEnrichJob {
  val byteLoad: DataLoadPointer[ArchEnrichRoot[_], Bytes] =
    DataLoadPointer(ByteLoad)

  val fileLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    DataLoadPointer(FileLoad)

  val plainTextLoad: DataLoadPointer[ArchEnrichRoot[_], String] =
    DataLoadPointer(PlainTextLoad)
}

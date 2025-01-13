package org.archive.webservices.ars.processing.jobs.archivespark

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkEnrichJob}
import org.archive.webservices.sparkling.io.StageSyncManager

abstract class AiJob extends ArchiveSparkEnrichJob {
//  override def maxInputSize: Int = 5000 // limit the input size to avoid extraordinarily long jobs

  override def enrich(
      rdd: RDD[ArchEnrichRoot[_]],
      conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = {
    StageSyncManager.sync(super.enrich(rdd, conf))
  }
}

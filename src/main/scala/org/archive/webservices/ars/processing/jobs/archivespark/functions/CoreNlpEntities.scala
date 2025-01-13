package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.functions.{Entities, EntitiesConstants}
import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.sparkling.io.StageSyncManager
import org.archive.webservices.sparkling.util.IteratorUtil

import java.util.Properties

class CoreNlpEntities(
    properties: Properties = EntitiesConstants.DefaultProps,
    filterLatin: Boolean = false) extends Entities(properties, filterLatin = filterLatin) {
  override def initPartition(partition: Iterator[EnrichRoot]): Iterator[EnrichRoot] = {
    StageSyncManager.lockMutex()
    super.initPartition(partition)
  }

  override def cleanup(): Unit = StageSyncManager.unlockMutex()

  override def enrichPartition[R <: EnrichRoot](partition: Iterator[R], func: EnrichFunc[R, _, _]): Iterator[R] = {
    IteratorUtil.preload(partition.map { r =>
      r.asInstanceOf[LocalFileCache].cachePayload()
      r
    }, numPreload = 50, parallelism = 2)(func.enrich)
  }
}

package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.functions.{Entities, EntitiesConstants}
import org.archive.webservices.archivespark.model.EnrichRoot
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.sparkling.io.StageSyncManager
import org.archive.webservices.sparkling.util.IteratorUtil

import java.util.Properties

class CoreNlpEntities(
    properties: Properties = EntitiesConstants.DefaultProps,
    filterLatin: Boolean = false) extends Entities(properties, cleanLatin = filterLatin) {
  override def initPartition[R <: EnrichRoot](partition: Iterator[R], init: Iterator[R] => Iterator[R], map: R => R): Iterator[R] = {
    StageSyncManager.lockMutex()
    IteratorUtil.preload(init(partition.map { r =>
      r.asInstanceOf[LocalFileCache].cachePayload()
      r
    }), numPreload = 50, parallelism = 2)(map)
  }

  override def cleanup(): Unit = {
    StageSyncManager.unlockMutex()
  }
}

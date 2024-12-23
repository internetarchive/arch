package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.functions.{Entities, EntitiesConstants}
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichable}
import org.archive.webservices.sparkling.io.StageSyncManager

import java.util.Properties

class CoreNlpEntities(
    properties: Properties = EntitiesConstants.DefaultProps,
    filterLatin: Boolean = false) extends Entities(properties, filterLatin = filterLatin) {
//  override def initPartition(partition: Iterator[EnrichRoot]): Iterator[EnrichRoot] = {
//    StageSyncManager.lockMutex()
//    super.initPartition(partition)
//  }
//
//  override def cleanup(): Unit = StageSyncManager.unlockMutex()

  override def derive(source: TypedEnrichable[String], derivatives: Derivatives): Unit = {
    StageSyncManager.stage.synchronized { // synchronize across cores of the same stage / the same job
      super.derive(source, derivatives)
    }
  }
}

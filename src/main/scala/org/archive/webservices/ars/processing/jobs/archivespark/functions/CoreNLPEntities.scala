package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.functions.{Entities, EntitiesConstants}
import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot, EnrichRootCompanion}
import org.archive.webservices.ars.processing.jobs.archivespark.base.PlainTextLoad
import org.archive.webservices.sparkling.io.StageSyncManager
import org.archive.webservices.sparkling.util.IteratorUtil

import java.util.Properties

class CoreNLPEntities(
    properties: Properties = EntitiesConstants.DefaultProps,
    filterLatin: Boolean = false)
    extends Entities(properties, filterLatin = filterLatin) {
//  override def enrichPartition[R <: EnrichRoot](
//      partition: Iterator[R],
//      func: EnrichFunc[R, _, _]): Iterator[R] = {
//    val initialized = partition.flatMap { r =>
//      r.companion.asInstanceOf[EnrichRootCompanion[R]].dataLoad(PlainTextLoad).map { func =>
//        func.init[R](r, excludeFromOutput = true)
//      }
//    }
//    IteratorUtil.preload(initialized, numPreload = 100, parallelism = 5)(func.enrich)
//  }
//
//  override def initPartition(partition: Iterator[EnrichRoot]): Iterator[EnrichRoot] = {
//    StageSyncManager.lockMutex()
//    super.initPartition(partition)
//  }
//
//  override def cleanup(): Unit = StageSyncManager.unlockMutex()
}

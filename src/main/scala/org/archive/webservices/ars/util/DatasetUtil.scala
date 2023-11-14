package org.archive.webservices.ars.util

import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.processing.{DerivationJobConf,DerivationJobInstance,JobManager}

object DatasetUtil {
  def formatId(collectionUserUrlId: String, job: DerivationJobInstance): String = {
    s"${collectionUserUrlId}:${if (job.conf.isSample) "1" else "0" }:${job.job.id}"
  }

  def parseId(datasetId: String, user: ArchUser): Option[(ArchCollection, DerivationJobInstance)] = {
    val Array(collectionId, isSample, jobId) = datasetId.reverse.split(":", 3).map(_.reverse).reverse
    val sample = if (isSample == "1") true else false
    for {
      collection <- ArchCollection.get(ArchCollection.userCollectionId(collectionId, user))
      job <- (
        JobManager
          .getInstanceOrGlobal(
            jobId,
            DerivationJobConf.collection(collection, sample = sample, global = false),
            DerivationJobConf.collection(collection, sample = sample, global = true))
      )
    } yield (collection, job)
  }
}

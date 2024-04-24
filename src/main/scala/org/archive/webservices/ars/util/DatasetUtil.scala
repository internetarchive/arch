package org.archive.webservices.ars.util

import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.processing.DerivationJob
import org.archive.webservices.ars.processing.JobManager.userJobs

object DatasetUtil {
  def formatId(collectionUserUrlId: String, job: DerivationJob): String = {
    collectionUserUrlId + ":" + job.id
  }

  def parseId(datasetId: String, user: ArchUser): Option[(ArchCollection, DerivationJob)] = {
    val (collectionId, jobId) = List(datasetId.splitAt(datasetId.lastIndexOf(":")))
      .map(x => (x._1, x._2.stripPrefix(":")))
      .head
    for {
      collection <- ArchCollection.get(ArchCollection.userCollectionId(collectionId, user));
      job <- userJobs.find(_.id == jobId)
    } yield (collection, job)
  }
}

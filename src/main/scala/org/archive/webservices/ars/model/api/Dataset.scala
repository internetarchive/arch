package org.archive.webservices.ars.model.api

import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.processing.DerivationJobInstance
import org.archive.webservices.ars.util.{DatasetUtil, FormatUtil}

case class Dataset(
    id: String,
    collectionId: String,
    collectionName: String,
    isSample: Boolean,
    jobId: String,
    category: String,
    name: String,
    sample: Int,
    state: String,
    startTime: Option[String],
    finishedTime: Option[String])
    extends ApiResponseObject[Dataset]

object Dataset {
  def apply(collection: ArchCollection, jobInstance: DerivationJobInstance)(implicit
      context: RequestContext): Dataset = {
    Dataset(
      id = DatasetUtil.formatId(collection.id, jobInstance),
      collectionId = collection.id,
      collectionName = collection.name,
      isSample = jobInstance.conf.isSample,
      jobId = jobInstance.job.uuid.toString,
      category = jobInstance.job.category.name,
      name = jobInstance.job.name,
      sample = jobInstance.conf.sample,
      state = jobInstance.stateStr,
      startTime = jobInstance.info.started.map(FormatUtil.instantTimeString),
      finishedTime = jobInstance.info.finished.map(FormatUtil.instantTimeString))
  }
}

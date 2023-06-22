package org.archive.webservices.ars.model.api

import io.circe._
import io.circe.syntax._

import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.util.{DatasetUtil, FormatUtil}
import org.archive.webservices.ars.processing.DerivationJobInstance

class Dataset(collection: ArchCollection, jobInstance: DerivationJobInstance, user: ArchUser) {
  val id = DatasetUtil.formatId(collection.userUrlId(user.id), jobInstance.job)
  val collectionId = collection.userUrlId(user.id)
  val collectionName = collection.name
  val isSample = (jobInstance.conf.sample != -1)
  val jobId = jobInstance.job.id
  val category = jobInstance.job.category.name
  val name = jobInstance.job.name
  val sample = jobInstance.conf.sample
  val state = jobInstance.stateStr
  val numFiles = jobInstance.outFiles.size
  val startTime = jobInstance.info.startTime.map(FormatUtil.instantTimeString)
  val finishedTime = jobInstance.info.finishedTime.map(FormatUtil.instantTimeString)
}

object Dataset {
  private def encode(ds: Dataset): Json = {
    Json.obj(
      "id" -> ds.id.asJson,
      "collectionId" -> ds.collectionId.asJson,
      "collectionName" -> ds.collectionName.asJson,
      "isSample" -> ds.isSample.asJson,
      "jobId" -> ds.jobId.asJson,
      "category" -> ds.category.asJson,
      "name" -> ds.name.asJson,
      "sample" -> ds.sample.asJson,
      "state" -> ds.state.asJson,
      "numFiles" -> ds.numFiles.asJson,
      "startTime" -> ds.startTime.asJson,
      "finishedTime" -> ds.finishedTime.asJson)
  }

  implicit val encodeDataset: Encoder[Dataset] = encode
}

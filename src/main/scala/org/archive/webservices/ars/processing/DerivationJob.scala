package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.ArsCloudJobCategory

import scala.concurrent.Future

trait DerivationJob {
  private val _id: String = getClass.getSimpleName.stripSuffix("$")
  def id: String = _id

  def name: String

  def category: ArsCloudJobCategory

  def description: String

  def templateName: Option[String] = Some("jobs/" + id)

  def run(conf: DerivationJobConf): Future[Boolean]

  def enqueue(
      conf: DerivationJobConf,
      get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] = {
    val instance = DerivationJobInstance(this, conf)
    instance.state = ProcessingState.Queued
    get(instance)
    Some(instance)
  }

  def history(conf: DerivationJobConf): DerivationJobInstance =
    JobManager.getRegistered(conf.collectionId, id).getOrElse {
      DerivationJobInstance(this, conf)
    }

  def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = Seq.empty
}

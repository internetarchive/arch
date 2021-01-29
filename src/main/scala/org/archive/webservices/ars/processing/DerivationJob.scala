package org.archive.webservices.ars.processing

import scala.concurrent.Future

trait DerivationJob {
  private val _id: String = getClass.getSimpleName.stripSuffix("$")
  def id: String = _id

  def name: String

  def templateName: Option[String] = Some("jobs/" + id)

  def run(conf: DerivationJobConf): Future[Boolean]

  def enqueue(conf: DerivationJobConf, get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] = {
    val instance = DerivationJobInstance(this, conf)
    instance.state = ProcessingState.Queued
    get(instance)
    Some(instance)
  }

  def history(conf: DerivationJobConf): DerivationJobInstance = JobManager.getRegistered(conf.collectionId, id).getOrElse {
    DerivationJobInstance(this, conf)
  }

  def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = Seq.empty
}

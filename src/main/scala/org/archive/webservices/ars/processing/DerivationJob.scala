package org.archive.webservices.ars.processing

import scala.concurrent.Future

trait DerivationJob {
  def id: String
  def name: String
  def templateName: String = "jobs/" + id
  def templateVariables(conf: DerivationJobConf): Seq[(String, Any)]
  def run(conf: DerivationJobConf): Future[Boolean]
  def enqueue(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = DerivationJobInstance(this, conf)
    instance.state = ProcessingState.Queued
    instance
  }
  def history(conf: DerivationJobConf): DerivationJobInstance = DerivationJobInstance(this, conf)
}

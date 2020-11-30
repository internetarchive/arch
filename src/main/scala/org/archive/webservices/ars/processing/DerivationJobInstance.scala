package org.archive.webservices.ars.processing

case class DerivationJobInstance(job: DerivationJob, conf: DerivationJobConf) {
  var state: Int = ProcessingState.NotStarted
  def stateStr: String = ProcessingState.Strings(state)
  def templateVariables: Seq[(String, Any)] = job.templateVariables(conf)
}
package org.archive.webservices.ars.processing

case class DerivationJobInstance(job: DerivationJob, conf: DerivationJobConf) {
  private var _state: Int = ProcessingState.NotStarted
  def state: Int = _state
  def state_=(value: Int): Unit = {
    _state = value
    for (func <- _onStateChanged) func()
  }

  def stateStr: String = ProcessingState.Strings(_state)
  def templateVariables: Seq[(String, Any)] = job.templateVariables(conf)

  private var _onStateChanged: Seq[() => Unit] = Seq.empty
  def onStateChanged(action: => Unit): Unit = _onStateChanged :+= (() => action)
}
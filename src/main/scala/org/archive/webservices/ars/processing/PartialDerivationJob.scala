package org.archive.webservices.ars.processing

abstract class PartialDerivationJob(parent: ChainedJob) extends DerivationJob {
  override def id: String = parent.id + "_" + super.id
  def name: String = id
  override def templateName: Option[String] = None
  override def enqueue(
      conf: DerivationJobConf,
      get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] =
    super.enqueue(conf, get)
}

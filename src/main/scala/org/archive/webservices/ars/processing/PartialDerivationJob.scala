package org.archive.webservices.ars.processing

abstract class PartialDerivationJob(parent: ChainedJob) extends DerivationJob {
  lazy val id: String = parent.id + "_sub-" + (parent.children.indexOf(this) + 1)
  def name: String = id
  override def templateName: Option[String] = None
  override def enqueue(conf: DerivationJobConf, get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] = super.enqueue(conf, get)
}

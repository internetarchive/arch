package org.archive.webservices.ars.processing
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}

abstract class PartialDerivationJob(parent: ChainedJob) extends DerivationJob {
  override val partialOf: Option[DerivationJob] = Some(parent)
  override lazy val id: String = parent.id + "_" + super.id
  def name: String = id
  def category: ArchJobCategory = ArchJobCategories.None
  def description: String = id
  override def templateName: Option[String] = None
  override def enqueue(
      conf: DerivationJobConf,
      get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] =
    super.enqueue(conf, get)
}

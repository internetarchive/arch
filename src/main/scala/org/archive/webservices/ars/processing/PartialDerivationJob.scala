package org.archive.webservices.ars.processing
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}

abstract class PartialDerivationJob(parent: ChainedJob) extends DerivationJob {
  override val partialOf: Option[DerivationJob] = Some(parent)
  override lazy val id: String = parent.id + "_" + super.id
  val name: String = id
  override lazy val uuid: String = parent.uuid
  override def relativeOutPath: String = parent.relativeOutPath
  val category: ArchJobCategory = ArchJobCategories.None
  val description: String = id
  override val templateName: Option[String] = None
  override def enqueue(
      conf: DerivationJobConf,
      get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] =
    super.enqueue(conf, get)
}

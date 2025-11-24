package org.archive.webservices.ars.processing.jobs.system

import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.{DerivationJob, DerivationJobConf}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DummyDerivationJob extends DerivationJob {
  override def name: String = id
  override def uuid: String = "01948861-c0ef-7bbb-b4fc-920c4324a644"
  override def relativeOutPath: String = ""
  override def category: ArchJobCategory = ArchJobCategories.System
  override def description: String = id
  override def run(instance: DerivationJobInstance): Future[Boolean] = Future(true)
}

package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.model.{ArchJobCategory, DerivativeOutput}

import scala.concurrent.Future

trait DerivationJob {
  val partialOf: Option[DerivationJob] = None

  private val _id: String = getClass.getSimpleName.stripSuffix("$")
  def id: String = _id

  def name: String

  def uuid: String

  def relativeOutPath: String

  val stage = "Processing"

  def category: ArchJobCategory

  def description: String

  def templateName: Option[String] = Some("jobs/" + id)

  def run(conf: DerivationJobConf): Future[Boolean]

  def enqueue(
      conf: DerivationJobConf,
      get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] = {
    val instance = DerivationJobInstance(this, conf)
    get(instance)
    Some(instance)
  }

  def history(uuid: String, conf: DerivationJobConf): DerivationJobInstance = {
    JobManager.getInstance(uuid).getOrElse(history(conf))
  }

  def history(conf: DerivationJobConf): DerivationJobInstance = {
    JobManager.getRegistered(id, conf).getOrElse {
      DerivationJobInstance(this, conf)
    }
  }

  def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = Seq.empty

  def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] = Iterator.empty

  def reset(conf: DerivationJobConf): Unit = {}

  def failedNotificationTemplate: Option[String] = Some("failed")

  def finishedNotificationTemplate: Option[String] = Some("finished")

  def logCollectionInfo: Boolean = JobManager.userJobs.contains(this)

  def validateParams(conf: DerivationJobConf): Option[String] = None

  def inputSize(conf: DerivationJobConf): Long = {
    if (InputSpec.isCollectionBased(conf.inputSpec)) {
      conf.inputSpec.collection.specifics.inputSize(conf)
    } else conf.inputSpec.size
  }
}

package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.collections.inputspecs.{InputSpec, InputSpecLoader}
import org.archive.webservices.ars.model.{ArchJobCategory, DerivativeOutput}
import org.archive.webservices.sparkling.io.HdfsIO

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

  def codeUrl: String =
    s"https://github.com/internetarchive/arch/blob/main/src/main/scala/${getClass.getName.replace(".", "/").stripSuffix("$")}.scala"

  def infoUrl: String =
    "https://arch-webservices.zendesk.com/hc/en-us/sections/14410598107028-ARCH-Datasets"

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

  def sampleVizData(conf: DerivationJobConf): Option[SampleVizData] = None

  def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = Seq.empty

  def datasetGlobMime(conf: DerivationJobConf): Option[(String, String)] = None

  def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] = {
    datasetGlobMime(conf).toIterator.flatMap { case (glob, mime) =>
      HdfsIO.files(glob).map { file =>
        val (path, name) = file.splitAt(file.lastIndexOf('/'))
        DerivativeOutput(name.stripPrefix("/"), path, mime.split('/').last, mime)
      }
    }
  }

  def reset(conf: DerivationJobConf): Unit = {}

  def failedNotificationTemplate: Option[String] = Some("failed")

  def finishedNotificationTemplate: Option[String] = Some("finished")

  def generatesOuputput: Boolean = true

  def logCollectionInfo: Boolean = JobManager.userJobs.contains(this) && generatesOuputput

  def validateParams(conf: DerivationJobConf): Option[String] = None

  def inputSize(conf: DerivationJobConf): Long = {
    if (InputSpec.isCollectionBased(conf.inputSpec)) {
      conf.inputSpec.collection.specifics.inputSize(conf)
    } else InputSpecLoader.size(conf.inputSpec)
  }

  def outputSize(conf: DerivationJobConf): Long = outFiles(conf).map(_.size).foldLeft(0L)(_ + _)
}

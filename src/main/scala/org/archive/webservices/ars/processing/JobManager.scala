package org.archive.webservices.ars.processing

import org.archive.webservices.ars.processing.jobs._

import scala.collection.immutable.ListMap
import scala.collection.mutable

object JobManager {
  private val instances =
    mutable.Map.empty[DerivationJobConf, mutable.Map[String, DerivationJobInstance]]

  private val collectionInstances =
    mutable.Map.empty[String, mutable.Set[DerivationJobInstance]]

  val userJobs: Seq[DerivationJob] = Seq(
    ArsLgaGeneration,
    ArsWaneGeneration,
    ArsWatGeneration,
    AudioInformationExtraction,
    DomainFrequencyExtraction,
    DomainGraphExtraction,
    FileCountAndSize,
    ImageGraphExtraction,
    ImageInformationExtraction,
    PdfInformationExtraction,
    PresentationProgramInformationExtraction,
    SpreadsheetInformationExtraction,
    TextFilesInformationExtraction,
    VideoInformationExtraction,
    WebGraphExtraction,
    WebPagesExtraction,
    WordProcessorInformationExtraction)

  val jobs: ListMap[String, DerivationJob] = ListMap(userJobs.sortBy(_.id).map { job =>
    job.id -> job
  }: _*)

  val nameLookup: Map[String, DerivationJob] = userJobs.map { job =>
    job.name -> job
  }.toMap

  def get(id: String): Option[DerivationJob] = jobs.get(id)

  def getByCollection(id: String): Set[DerivationJobInstance] =
    collectionInstances.get(id).map(_.toSet).getOrElse(Set.empty)

  def register(instance: DerivationJobInstance): Boolean = instances.synchronized {
    val conf = instance.conf
    val confJobs = instances.getOrElseUpdate(conf, mutable.Map.empty)
    if (!confJobs.contains(instance.job.id)) {
      confJobs.update(instance.job.id, instance)
      if (instance.job.partialOf.isEmpty) {
        val collectionJobs =
          collectionInstances.getOrElseUpdate(conf.collectionId, mutable.Set.empty)
        collectionJobs.add(instance)
      }
      instance.registered = true
      JobStateManager.logRegister(instance)
      true
    } else false
  }

  def unregister(instance: DerivationJobInstance): Boolean = instances.synchronized {
    val conf = instance.conf
    val confJobs = instances.get(conf)
    val removed = confJobs.flatMap(_.remove(instance.job.id))
    if (removed.isDefined) {
      if (confJobs.get.isEmpty) instances.remove(conf)
      if (removed.get.job.partialOf.isEmpty) {
        for (collectionJobs <- collectionInstances.get(conf.collectionId)) {
          collectionJobs.remove(removed.get)
          if (collectionJobs.isEmpty) collectionInstances.remove(conf.collectionId)
        }
      }
      instance.registered = false
      JobStateManager.logUnregister(instance)
      true
    } else false
  }

  def getInstance(jobId: String, conf: DerivationJobConf): Option[DerivationJobInstance] =
    getRegistered(jobId, conf).orElse {
      jobs.get(jobId).map { job =>
        job.history(conf)
      }
    }

  def getInstanceOrGlobal(jobId: String, conf: DerivationJobConf, globalConf: => Option[DerivationJobConf]): Option[DerivationJobInstance] = {
    val instance = JobManager.getInstance(jobId, conf)
    if (instance.isEmpty || instance.exists(_.state == ProcessingState.NotStarted)) {
      val global = globalConf.filter(_.outputPath != conf.outputPath).flatMap { conf =>
        JobManager.getInstance(jobId, conf)
      }
      if (global.exists(_.state > ProcessingState.NotStarted)) global else instance
    } else instance
  }

  def getRegistered(jobId: String, conf: DerivationJobConf): Option[DerivationJobInstance] =
    instances.get(conf).flatMap(_.get(jobId))

  def registered(conf: DerivationJobConf): Seq[DerivationJobInstance] =
    instances.get(conf).toSeq.flatMap(_.values)
}

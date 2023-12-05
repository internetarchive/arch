package org.archive.webservices.ars.processing

import org.archive.webservices.ars.processing.jobs._
import org.archive.webservices.ars.processing.jobs.archivespark.ArchiveSparkEntitiesExtraction
import org.archive.webservices.ars.processing.jobs.system.{DatasetPublication, UserDefinedQuery}

import scala.collection.immutable.ListMap
import scala.collection.mutable

object JobManager {
  var MaxAttempts = 3
  var MaxSlots = 3

  private val instances =
    mutable.Map.empty[DerivationJobConf, mutable.Map[String, DerivationJobInstance]]

  private val collectionInstances =
    mutable.Map.empty[String, mutable.Set[DerivationJobInstance]]

  val userJobs: Set[DerivationJob] = Set(
    ArchiveSparkEntitiesExtraction,
    ArsLgaGeneration,
    ArsWaneGeneration,
    ArsWatGeneration,
    AudioInformationExtraction,
    DomainFrequencyExtraction,
    DomainGraphExtraction,
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

  val systemJobs: Set[DerivationJob] = Set(UserDefinedQuery, DatasetPublication)

  val jobs: ListMap[String, DerivationJob] = ListMap(
    (userJobs ++ systemJobs).toSeq.sortBy(_.id).map { job =>
      job.id -> job
    }: _*)

  val nameLookup: Map[String, DerivationJob] = jobs.values.map { job =>
    job.name -> job
  }.toMap

  def get(id: String): Option[DerivationJob] = jobs.get(id)

  def getByCollection(id: String): Set[DerivationJobInstance] =
    collectionInstances.get(id).map(_.toSet).getOrElse(Set.empty)

  def registered: Set[DerivationJobInstance] = collectionInstances.values.flatten.toSet

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
      instance.unregistered()
      if (instance.job.partialOf.isEmpty && instance.state == ProcessingState.Failed && instance.attempt < MaxAttempts) {
        instance.job.reset(instance.conf)
        instance.job.enqueue(
          instance.conf,
          { newInstance =>
            newInstance.user = instance.user
            newInstance.collection = instance.collection
            newInstance.attempt = instance.attempt + 1
            if (instance.slots < MaxSlots) newInstance.slots = instance.slots + 1
          })
      }
      true
    } else false
  }

  def getInstance(jobId: String, conf: DerivationJobConf): Option[DerivationJobInstance] =
    getRegistered(jobId, conf).orElse {
      jobs.get(jobId).map { job =>
        job.history(conf)
      }
    }

  def getInstanceOrGlobal(
      jobId: String,
      conf: DerivationJobConf,
      globalConf: => DerivationJobConf): Option[DerivationJobInstance] = {
    val instance = JobManager.getInstance(jobId, conf)
    if (instance.isEmpty || instance.exists(_.state == ProcessingState.NotStarted)) {
      val global = Some(globalConf).filter(_.outputPath != conf.outputPath).flatMap { conf =>
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

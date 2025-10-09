package org.archive.webservices.ars.processing

import org.archive.webservices.ars.io.UuidManager
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.processing.jobs._
import org.archive.webservices.ars.processing.jobs.archivespark._
import org.archive.webservices.ars.processing.jobs.archivespark.preset._
import org.archive.webservices.ars.processing.jobs.system.{DatasetPublication, MetadataSummary, UserDefinedQuery}

import scala.collection.immutable.ListMap
import scala.collection.mutable

object JobManager {
  var MaxAttempts = 3
  var MaxSlots = 3

  private val confInstances =
    mutable.Map.empty[DerivationJobConf.Identifier, mutable.Map[String, DerivationJobInstance]]
  private val uuidInstances = mutable.Map.empty[String, DerivationJobInstance]
  // to be removed when ARCH becomes pure IO job processor
  private val collectionInstances = mutable.Map.empty[String, mutable.Set[DerivationJobInstance]]

  var userJobs: Set[DerivationJob] = Set(
    ArchiveSparkFlexJob,
    ArchiveSparkNoop,
    EntityExtraction,
    WhisperTranscription,
    WhisperText,
    WhisperEntityExtraction,
    TrOcrProcessing,
    TrOcrEntityExtraction,
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
    WordProcessorInformationExtraction,
    HtmlTextExtraction,
    PySmokeTestJob)

  var systemJobs: Set[DerivationJob] = Set(UserDefinedQuery, DatasetPublication, MetadataSummary)

  private var _jobs: Option[ListMap[String, DerivationJob]] = None
  def jobs: ListMap[String, DerivationJob] = _jobs.getOrElse {
    _jobs = Some(ListMap(
      (userJobs ++ systemJobs).toSeq.sortBy(_.id).map { job =>
        job.id -> job
      }: _*))
    _jobs.get
  }

  private var _nameLookup: Option[Map[String, DerivationJob]] = None
  def nameLookup: Map[String, DerivationJob] = _nameLookup.getOrElse {
    _nameLookup = Some(jobs.values.map { job =>
      job.name -> job
    }.toMap)
    _nameLookup.get
  }

  private var _uuidLookup: Option[Map[String, DerivationJob]] = None
  def uuidLookup: Map[String, DerivationJob] = _uuidLookup.getOrElse {
    _uuidLookup = Some(jobs.values.map { job =>
      job.uuid -> job
    }.toMap)
    _uuidLookup.get
  }

  def get(idOrUuid: String): Option[DerivationJob] =
    jobs.get(uuidLookup.get(idOrUuid).map(_.id).getOrElse(idOrUuid))

  def getCollectionInstances(collectionId: String): Set[DerivationJobInstance] = {
    collectionInstances.get(collectionId).map(_.toSet).getOrElse(Set.empty)
  }

  def registerUuid(instance: DerivationJobInstance): Unit = {
    if (instance.job.partialOf.isEmpty) {
      uuidInstances(instance.uuid) = instance
      UuidManager.saveUuidInfo(instance)
    }
  }

  def getInstance(uuid: String): Option[DerivationJobInstance] = {
    uuidInstances.get(uuid).orElse {
      UuidManager.getInstance(uuid)
    }
  }

  def registered: Set[DerivationJobInstance] = collectionInstances.values.flatten.toSet

  def register(instance: DerivationJobInstance): Boolean = confInstances.synchronized {
    val conf = instance.conf
    val confJobs = confInstances.getOrElseUpdate(conf, mutable.Map.empty)
    if (!confJobs.contains(instance.job.id)) {
      confJobs.update(instance.job.id, instance)
      if (instance.job.partialOf.isEmpty) {
        registerUuid(instance)
        if (InputSpec.isCollectionBased(conf.inputSpec)) {
          val collectionJobs =
            collectionInstances.getOrElseUpdate(conf.inputSpec.collectionId, mutable.Set.empty)
          collectionJobs.add(instance)
        }
        JobStateManager.logRegister(instance)
      }
      instance.registered = true
      true
    } else false
  }

  def unregister(instance: DerivationJobInstance): Unit = confInstances.synchronized {
    val conf = instance.conf
    val confJobs = confInstances.get(conf)
    for (jobs <- confJobs) {
      jobs.remove(instance.job.id)
      if (jobs.isEmpty) confInstances.remove(conf)
    }
    val subjob = instance.job.partialOf.isDefined
    if (!subjob) {
      uuidInstances.remove(instance.uuid)
      if (InputSpec.isCollectionBased(conf.inputSpec)) {
        for (collectionJobs <- collectionInstances.get(conf.inputSpec.collectionId)) {
          collectionJobs.remove(instance)
          if (collectionJobs.isEmpty) collectionInstances.remove(conf.inputSpec.collectionId)
        }
      }
      JobStateManager.logUnregister(instance)
    }
    instance.registered = false
    instance.unregistered()
    if (!subjob) {
      if (instance.state == ProcessingState.Failed && instance.attempt < MaxAttempts) {
        if (!instance.isResumable) DerivationJob.reset(instance.job, instance.conf)
        instance.job.enqueue(
          instance.conf,
          { newInstance =>
            newInstance.predefUuid = Some(instance.uuid)
            newInstance.user = instance.user
            newInstance.attempt = instance.attempt + 1
            if (instance.slots < MaxSlots) newInstance.slots = instance.slots + 1
          })
      } else if (instance.state == ProcessingState.Finished) {
        UuidManager.finalizeUuid(instance)
      }
    }
  }

  def getInstance(jobIdOrUuid: String, conf: DerivationJobConf): Option[DerivationJobInstance] = {
    val jobId = uuidLookup.get(jobIdOrUuid).map(_.id).getOrElse(jobIdOrUuid)
    getRegistered(jobId, conf).orElse {
      jobs.get(jobId).map { job =>
        job.history(conf)
      }
    }
  }

  def getInstanceOrGlobal(
      jobIdOrUuid: String,
      conf: DerivationJobConf,
      globalConf: => Option[DerivationJobConf]): Option[DerivationJobInstance] = {
    val jobId = uuidLookup.get(jobIdOrUuid).map(_.id).getOrElse(jobIdOrUuid)
    val instance = JobManager.getInstance(jobId, conf)
    if (instance.isEmpty || instance.exists(_.state == ProcessingState.NotStarted)) {
      val global = globalConf.filter(_.outputPath != conf.outputPath).flatMap { conf =>
        JobManager.getInstance(jobId, conf)
      }
      if (global.exists(_.state > ProcessingState.NotStarted)) global else instance
    } else instance
  }

  def getRegistered(jobId: String, conf: DerivationJobConf): Option[DerivationJobInstance] =
    confInstances.get(conf).flatMap(_.get(jobId))

  def registered(conf: DerivationJobConf): Seq[DerivationJobInstance] =
    confInstances.get(conf).toSeq.flatMap(_.values)
}

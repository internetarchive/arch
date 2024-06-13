package org.archive.webservices.ars.processing

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.processing.jobs._
import org.archive.webservices.ars.processing.jobs.archivespark._
import org.archive.webservices.ars.processing.jobs.archivespark.preset.{EntityExtraction, EntityExtractionChinese, WhisperEntityExtraction, WhisperTranscription}
import org.archive.webservices.ars.processing.jobs.system.{DatasetPublication, MetadataSummary, UserDefinedQuery}
import org.archive.webservices.sparkling.io.HdfsIO

import scala.collection.immutable.ListMap
import scala.collection.mutable

object JobManager {
  var MaxAttempts = 3
  var MaxSlots = 3
  val InstanceUuidFileSuffix = ".uuid.json"

  private val confInstances =
    mutable.Map.empty[DerivationJobConf.Identifier, mutable.Map[String, DerivationJobInstance]]
  private val uuidInstances = mutable.Map.empty[String, DerivationJobInstance]
  // to be removed when ARCH becomes pure IO job processor
  private val collectionInstances = mutable.Map.empty[String, mutable.Set[DerivationJobInstance]]

  val userJobs: Set[DerivationJob] = Set(
    ArchiveSparkFlexJob,
    ArchiveSparkNoop,
    EntityExtraction,
    EntityExtractionChinese,
    WhisperTranscription,
    WhisperEntityExtraction,
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

  val systemJobs: Set[DerivationJob] = Set(UserDefinedQuery, DatasetPublication, MetadataSummary)

  val jobs: ListMap[String, DerivationJob] = ListMap(
    (userJobs ++ systemJobs).toSeq.sortBy(_.id).map { job =>
      job.id -> job
    }: _*)

  val nameLookup: Map[String, DerivationJob] = jobs.values.map { job =>
    job.name -> job
  }.toMap

  val uuidLookup: Map[String, DerivationJob] = jobs.values.map { job =>
    job.uuid -> job
  }.toMap

  def get(idOrUuid: String): Option[DerivationJob] =
    jobs.get(uuidLookup.get(idOrUuid).map(_.id).getOrElse(idOrUuid))

  def getCollectionInstances(collectionId: String): Set[DerivationJobInstance] = {
    collectionInstances.get(collectionId).map(_.toSet).getOrElse(Set.empty)
  }

  def registerUuid(instance: DerivationJobInstance): Unit =
    if (instance.job.partialOf.isEmpty) {
      uuidInstances(instance.uuid) = instance
      val uuidPath = instance.conf.outputPath + "/" + instance.uuid + InstanceUuidFileSuffix
      HdfsIO.writeLines(
        uuidPath,
        Seq(
          (ListMap(
            "jobUuid" -> instance.job.uuid.asJson,
            "jobId" -> instance.job.id.asJson,
            "conf" -> instance.conf.toJson) ++ {
            instance.user.map("user" -> _.id.asJson)
          }).asJson.spaces4),
        overwrite = true)
    }

  def getInstance(uuid: String): Option[DerivationJobInstance] = {
    uuidInstances.get(uuid).orElse {
      ArchConf.uuidJobOutPath
        .map(_ + "/" + uuid)
        .map { uuidPath =>
          uuidPath + "/" + uuid + InstanceUuidFileSuffix
        }
        .filter(HdfsIO.exists)
        .flatMap { uuidFilePath =>
          parse(HdfsIO.lines(uuidFilePath).mkString).toOption.map(_.hcursor).flatMap { cursor =>
            for {
              job <- cursor.get[String]("jobUuid").toOption.flatMap(uuidLookup.get)
              conf <- cursor.downField("conf").focus.flatMap(DerivationJobConf.fromJson)
            } yield {
              val instance = job.history(conf)
              instance.user = cursor.get[String]("user").toOption.flatMap(ArchUser.get(_))
              instance
            }
          }
        }
    }
  }

  def registered: Set[DerivationJobInstance] = collectionInstances.values.flatten.toSet

  def register(instance: DerivationJobInstance): Boolean = confInstances.synchronized {
    val conf = instance.conf
    val confJobs = confInstances.getOrElseUpdate(conf, mutable.Map.empty)
    if (!confJobs.contains(instance.job.id)) {
      confJobs.update(instance.job.id, instance)
      registerUuid(instance)
      if (instance.job.partialOf.isEmpty && InputSpec.isCollectionBased(conf.inputSpec)) {
        val collectionJobs =
          collectionInstances.getOrElseUpdate(conf.inputSpec.collectionId, mutable.Set.empty)
        collectionJobs.add(instance)
      }
      instance.registered = true
      JobStateManager.logRegister(instance)
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
    uuidInstances.remove(instance.uuid)
    if (instance.job.partialOf.isEmpty && InputSpec.isCollectionBased(conf.inputSpec)) {
      for (collectionJobs <- collectionInstances.get(conf.inputSpec.collectionId)) {
        collectionJobs.remove(instance)
        if (collectionJobs.isEmpty) collectionInstances.remove(conf.inputSpec.collectionId)
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
          newInstance.predefUuid = Some(instance.uuid)
          newInstance.user = instance.user
          newInstance.attempt = instance.attempt + 1
          if (instance.slots < MaxSlots) newInstance.slots = instance.slots + 1
        })
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

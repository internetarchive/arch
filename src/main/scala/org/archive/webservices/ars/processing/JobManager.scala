package org.archive.webservices.ars.processing

import org.archive.webservices.ars.processing.jobs.{
  AudioInformationExtraction,
  DomainFrequencyExtraction,
  DomainGraphExtraction,
  FileCountAndSize,
  ImageGraphExtraction,
  ImageInformationExtraction,
  PdfInformationExtraction,
  PresentationProgramInformationExtraction,
  TextFilesInformationExtraction,
  SpreadsheetInformationExtraction,
  VideoInformationExtraction,
  WebGraphExtraction,
  WebPagesExtraction,
  WordProcessorInformationExtraction
}

import scala.collection.immutable.ListMap
import scala.collection.mutable

object JobManager {
  private val instances =
    mutable.Map.empty[DerivationJobConf, mutable.Map[String, DerivationJobInstance]]

  val registeredJobs: Seq[DerivationJob] = Seq(
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

  val jobs: ListMap[String, DerivationJob] = ListMap(registeredJobs.sortBy(_.id).map { job =>
    job.id -> job
  }: _*)

  def get(id: String): Option[DerivationJob] = jobs.get(id)

  def register(instance: DerivationJobInstance): Boolean = instances.synchronized {
    val collectionJobs = instances.getOrElseUpdate(instance.conf, mutable.Map.empty)
    if (!collectionJobs.contains(instance.job.id)) {
      collectionJobs.update(instance.job.id, instance)
      println(
        "Registered job " + instance.job.id + " (" + instance.hashCode.abs + ") [" + instance.conf + "]")
      true
    } else false
  }

  def unregister(instance: DerivationJobInstance): Boolean = instances.synchronized {
    val collectionJobs = instances.get(instance.conf)
    if (collectionJobs.isDefined) {
      val removed = collectionJobs.get.remove(instance.job.id)
      if (removed.nonEmpty) {
        if (collectionJobs.get.isEmpty) instances.remove(instance.conf)
        println(
          "Unregistered job " + instance.job.id + " (" + instance.hashCode.abs + ") [" + instance.conf + "]")
        true
      } else false
    } else false
  }

  def getInstance(jobId: String, conf: DerivationJobConf): Option[DerivationJobInstance] =
    getRegistered(jobId, conf).orElse {
      jobs.get(jobId).map { job =>
        job.history(conf)
      }
    }

  def getRegistered(jobId: String, conf: DerivationJobConf): Option[DerivationJobInstance] =
    instances.get(conf).flatMap(_.get(jobId))

  def registered(conf: DerivationJobConf): Seq[DerivationJobInstance] =
    instances.get(conf).toSeq.flatMap(_.values)
}

package org.archive.webservices.ars.processing

import org.archive.webservices.ars.processing.jobs.{FileCountAndSize, AudioInformationExtraction, DomainFrequencyExtraction, WebPagesExtraction}

import scala.collection.immutable.ListMap
import scala.collection.mutable

object JobManager {
  private val instances = mutable.Map.empty[String, mutable.Map[String, DerivationJobInstance]]

  val registeredJobs: Seq[DerivationJob] = Seq(FileCountAndSize, AudioInformationExtraction, DomainFrequencyExtraction, WebPagesExtraction)

  val jobs: ListMap[String, DerivationJob] = ListMap(registeredJobs.sortBy(_.id).map { job =>
    job.id -> job
  }: _*)

  def get(id: String): Option[DerivationJob] = jobs.get(id)

  def register(instance: DerivationJobInstance): Boolean = {
    instances.synchronized {
      val collectionJobs = instances.getOrElseUpdate(instance.conf.collectionId, mutable.Map.empty)
      if (!collectionJobs.contains(instance.job.id)) {
        collectionJobs.update(instance.job.id, instance)
        true
      } else false
    }
  }

  def unregister(instance: DerivationJobInstance): Boolean = {
    instances.synchronized {
      val collectionJobs = instances.get(instance.conf.collectionId)
      if (collectionJobs.isDefined) {
        val removed = collectionJobs.get.remove(instance.job.id)
        if (collectionJobs.get.isEmpty) instances.remove(instance.conf.collectionId)
        removed.isDefined
      } else false
    }
  }

  def getInstance(collectionId: String, jobId: String): Option[DerivationJobInstance] = getRegistered(collectionId, jobId).orElse {
    jobs.get(jobId).flatMap { job =>
      DerivationJobConf.collection(collectionId).map { conf =>
        job.history(conf)
      }
    }
  }

  def getRegistered(collectionId: String, jobId: String): Option[DerivationJobInstance] = instances.get(collectionId).flatMap(_.get(jobId))

  def registered(collectionId: String): Seq[DerivationJobInstance] = instances.get(collectionId).toSeq.flatMap(_.values)
}

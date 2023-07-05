package org.archive.webservices.ars.processing.jobs.system

import org.archive.webservices.ars.io.CollectionAccessContext
import org.archive.webservices.ars.model._
import org.archive.webservices.ars.processing._
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.logging.LogContext
import org.archive.webservices.sparkling.util.RddUtil

import scala.concurrent.Future

object DatasetPublication extends SparkJob {
  implicit val logContext: LogContext = LogContext(this)

  val name = "Dataset publication"
  val category: ArchJobCategory = ArchJobCategories.System
  def description = "Job to publish a dataset on archive.org (internal system job)"

  override def validateParams(
      collection: ArchCollection,
      conf: DerivationJobConf): Option[String] =
    super
      .validateParams(collection, conf)
      .orElse {
        conf.params.get[String]("dataset") match {
          case Some(jobId) =>
            PublishedDatasets.ProhibitedJobs.find(_.id == jobId) match {
              case Some(_) => Some("Derivation job " + jobId + " prohibited to be published.")
              case None =>
                PublishedDatasets.dataset(jobId, collection, conf.isSample) match {
                  case Some(_) => None
                  case None => Some("Derivation job " + jobId + " not found.")
                }
            }
          case None => Some("No dataset specified.")
        }
      }
      .orElse {
        conf.params.values.get("metadata") match {
          case Some(json) =>
            PublishedDatasets.validateMetadata(PublishedDatasets.parseJsonMetadata(json))
          case None => Some("No metadata specified.")
        }
      }

  def run(conf: DerivationJobConf): Future[Boolean] = {
    for {
      jobId <- conf.params.get[String]("dataset")
      collection <- ArchCollection.get(conf.collectionId)
      dataset <- PublishedDatasets.dataset(jobId, collection, conf.isSample)
      metadata <- conf.params.values.get("metadata").map(PublishedDatasets.parseJsonMetadata)
      itemInfo <- PublishedDatasets.publish(jobId, collection, conf.isSample, metadata)
    } yield {
      if (itemInfo.complete) Future(true)
      else {
        val itemName = itemInfo.item
        SparkJobManager.context.map { sc =>
          SparkJobManager.initThread(sc, DatasetPublication, conf)
          val accessContext = CollectionAccessContext.fromLocalArchConf
          val fileListBc = sc.broadcast(PublishedDatasets.files(itemName))
          RddUtil
            .parallelize(dataset.outFiles.map(f => (f.filename, f.path)).toList)
            .foreachPartition { partition =>
              accessContext.init()
              val fileList = fileListBc.value
              for ((f, p) <- partition if !fileList.contains(f)) {
                if (!PublishedDatasets.upload(itemName, f, p)) {
                  throw new RuntimeException(s"Uploading $f to Petabox item $itemName failed.")
                }
              }
            }
          PublishedDatasets.complete(collection, dataset, itemName)
        }
      }
    }
  }.getOrElse(Future(false))

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    for {
      jobId <- conf.params.get[String]("dataset")
      collection <- ArchCollection.get(conf.collectionId)
      dataset <- PublishedDatasets.dataset(jobId, collection, conf.isSample)
    } {
      val jobFilePath = PublishedDatasets.jobFile(dataset)
      for (info <- PublishedDatasets.jobItem(jobFilePath)) {
        instance.state = if (info.complete) ProcessingState.Finished else ProcessingState.Failed
      }
    }
    instance
  }

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] = Iterator.empty

  override val templateName: Option[String] = None

  override def reset(conf: DerivationJobConf): Unit = {}

  override val finishedNotificationTemplate: Option[String] = None

  override val logJobInfo: Boolean = true
}

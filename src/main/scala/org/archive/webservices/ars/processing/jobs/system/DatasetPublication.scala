package org.archive.webservices.ars.processing.jobs.system

import org.archive.webservices.ars.io.FileAccessContext
import org.archive.webservices.ars.model._
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.processing._
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.util.RddUtil

import scala.concurrent.Future

object DatasetPublication extends SparkJob {
  val name = "Dataset publication"
  val uuid = "018950a2-21cb-7034-8d2a-03dff990cc1a"
  val category: ArchJobCategory = ArchJobCategories.System
  def description = "Job to publish a dataset on archive.org (internal system job)"

  override def relativeOutPath: String = throw new UnsupportedOperationException()

  private def confDataset(conf: DerivationJobConf): Either[String, DerivationJobInstance] = {
    val datasetParam = conf.params.get[String]("dataset")
    if (datasetParam.isEmpty && !InputSpec.isDatasetBased(conf.inputSpec))
      return Left("No dataset specified.")
    lazy val dataset = {
      if (datasetParam.isDefined) {
        datasetParam.flatMap(PublishedDatasets.dataset(_, conf)).getOrElse {
          return Left("Derivation job " + datasetParam.get + " not found.")
        }
      } else conf.inputSpec.dataset
    }
    val jobId = datasetParam.getOrElse(dataset.job.id)
    if (PublishedDatasets.ProhibitedJobs.exists(_.id == jobId)) {
      Left("Derivation job " + jobId + " is prohibited to be published.")
    } else Right(dataset)
  }

  override def validateParams(conf: DerivationJobConf): Option[String] = {
    super
      .validateParams(conf)
      .orElse {
        confDataset(conf).left.toOption
      }
      .orElse {
        conf.params.values.get("metadata") match {
          case Some(json) =>
            PublishedDatasets.validateMetadata(PublishedDatasets.parseJsonMetadata(json))
          case None => Some("No metadata specified.")
        }
      }
  }

  def run(conf: DerivationJobConf): Future[Boolean] = {
    for {
      dataset <- confDataset(conf).toOption
      metadata <- conf.params.values.get("metadata").map(PublishedDatasets.parseJsonMetadata)
      itemInfo <- PublishedDatasets.publish(dataset, metadata)
    } yield {
      if (itemInfo.complete) Future(true)
      else {
        val itemName = itemInfo.item
        SparkJobManager.context.map { sc =>
          SparkJobManager.initThread(sc, DatasetPublication, conf)
          val accessContext = FileAccessContext.fromLocalArchConf
          val fileListBc = sc.broadcast(PublishedDatasets.files(itemName))
          RddUtil
            .parallelize(dataset.outFiles.map(f => (f.filename, f.path)).toList)
            .foreachPartition { partition =>
              accessContext.init()
              val fileList = fileListBc.value
              for {
                (f, p) <- partition if !fileList.contains(f)
                error <- PublishedDatasets.upload(itemName, f, p)
              } {
                throw new RuntimeException(
                  s"Uploading $f to Petabox item $itemName failed. - $error")
              }
            }
          PublishedDatasets.complete(dataset, itemName)
        }
      }
    }
  }.getOrElse(Future(false))

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    for (dataset <- confDataset(conf).toOption) {
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

  override def generatesOuputput: Boolean = false
}

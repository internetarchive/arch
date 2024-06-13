package org.archive.webservices.ars.processing.jobs.system

import org.archive.webservices.ars.model.collections.inputspecs.InputSpecLoader
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaSummary
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, ArchJobInstanceInfo, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.io._

import scala.concurrent.Future

object MetadataSummary extends SparkJob {
  val name = "Metadata Summary"
  val uuid = "4a3fae37-99de-4a64-843d-bce3a44807b1"
  val category: ArchJobCategory = ArchJobCategories.System
  def description = "Summarizes metadata of a given input spec"

  val relativeOutPath: String = s"/$id"

  val SummaryFile = "summary.json"

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { sc =>
      SparkJobManager.initThread(sc, MetadataSummary, conf)
      InputSpecLoader.loadSpark(conf.inputSpec) { rdd =>
        val summary = rdd.mapPartitions { partition =>
          val summary = new FileMetaSummary()
          for (f <- partition) summary.add(f.meta)
          Iterator(summary)
        }.fold(FileMetaSummary.empty)(_ ++ _)
        HdfsIO.writeLines(conf.outputPath + relativeOutPath + "/" + SummaryFile, lines = Seq(summary.toJsonSchema.spaces4))
        true
      }
    }
  }

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    val started = HdfsIO.exists(conf.outputPath + relativeOutPath + "/" + ArchJobInstanceInfo.InfoFile)
    if (started) {
      val completed = HdfsIO.exists(conf.outputPath + relativeOutPath + "/" + SummaryFile)
      instance.state = if (completed) ProcessingState.Finished else ProcessingState.Failed
    }
    instance
  }

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] = Iterator(
    DerivativeOutput(SummaryFile, conf.outputPath + relativeOutPath, "JSON", "application/json")
  )

  override val templateName: Option[String] = Some("jobs/DefaultArsJob")

  override def reset(conf: DerivationJobConf): Unit = HdfsIO.delete(conf.outputPath + relativeOutPath)
}

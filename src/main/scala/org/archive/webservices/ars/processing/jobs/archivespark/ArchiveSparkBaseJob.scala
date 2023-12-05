package org.archive.webservices.ars.processing.jobs.archivespark

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.ArchiveSpark
import org.archive.webservices.archivespark.dataspecs.DataSpec
import org.archive.webservices.archivespark.model.EnrichRoot
import org.archive.webservices.ars.io.{CollectionLoader, IOHelper}
import org.archive.webservices.ars.model.DerivativeOutput
import org.archive.webservices.ars.model.collections.filespecs.FileRecord
import org.archive.webservices.ars.model.collections.{CollectionSpecifics, FileCollectionSpecifics}
import org.archive.webservices.ars.processing._
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.warc.WarcRecord

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ArchiveSparkBaseJob[Root <: EnrichRoot : ClassTag] extends SparkJob {
  val relativeOutPath = s"/$id"
  val resultDir = "/out.json.gz"

  def filterEnrich(rdd: RDD[Root]): RDD[Root]

  def filterEnrichSave(rdd: RDD[Root], conf: DerivationJobConf): Unit = {
    filterEnrich(rdd).saveAsJson(conf.outputPath + relativeOutPath + resultDir)
  }

  def loadFilterEnrichSave(spec: DataSpec[_, Root], conf: DerivationJobConf): Unit = {
    filterEnrichSave(ArchiveSpark.load(spec), conf)
  }

  def warcSpec(rdd: RDD[WarcRecord]): DataSpec[_, Root]

  def fileSpec(rdd: RDD[FileRecord]): DataSpec[_, Root]

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { sc =>
      SparkJobManager.initThread(sc, this, conf)
      if (conf.collectionId.startsWith(FileCollectionSpecifics.Prefix)) {
        CollectionSpecifics.get(conf.collectionId).map { specifics =>
          specifics.loadFiles(conf.inputPath) { rdd =>
            IOHelper.sample(rdd, conf.sample) { rdd =>
              loadFilterEnrichSave(fileSpec(rdd), conf)
              true
            }
          }
        }.getOrElse {
          throw new UnsupportedOperationException()
        }
      } else {
        CollectionLoader.loadWarcs(conf.collectionId, conf.inputPath) { rdd =>
          IOHelper.sample(rdd, conf.sample) { rdd =>
            loadFilterEnrichSave(warcSpec(rdd), conf)
            true
          }
        }
      }
    }
  }

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    val started = HdfsIO.exists(conf.outputPath + relativeOutPath + resultDir)
    if (started) {
      val completed =
        HdfsIO.exists(
          conf.outputPath + relativeOutPath + resultDir + "/" + Sparkling.CompleteFlagFile)
      instance.state = if (completed) ProcessingState.Finished else ProcessingState.Failed
    }
    instance
  }

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] =
    HdfsIO.files(conf.outputPath + relativeOutPath + resultDir + "/*.gz").map { file =>
      val (path, name) = file.splitAt(file.lastIndexOf('/'))
      DerivativeOutput(name.stripPrefix("/"), path, "wane", "application/gzip")
    }

  override val templateName: Option[String] = Some("jobs/DefaultArsJob")

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)
}

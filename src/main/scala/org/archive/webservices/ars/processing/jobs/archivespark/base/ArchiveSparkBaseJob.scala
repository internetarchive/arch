package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.ArchiveSpark
import org.archive.webservices.archivespark.dataspecs.DataSpec
import org.archive.webservices.archivespark.model.EnrichRoot
import org.archive.webservices.ars.WasapiController
import org.archive.webservices.ars.io.{IOHelper, WebArchiveLoader}
import org.archive.webservices.ars.model.collections.inputspecs.{FileRecord, InputSpec, InputSpecLoader}
import org.archive.webservices.ars.model.{ArchConf, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.warc.WarcRecord

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ArchiveSparkBaseJob[Root <: EnrichRoot: ClassTag] extends SparkJob {
  val relativeOutPath = s"/$id"
  val resultDir = "/out.json.gz"

  def filterEnrich(rdd: RDD[Root], conf: DerivationJobConf): RDD[Root]

  def filterEnrichSave(rdd: RDD[Root], conf: DerivationJobConf): Unit = {
    filterEnrich(rdd, conf).saveAsJson(conf.outputPath + relativeOutPath + resultDir)
  }

  def loadFilterEnrichSave(spec: DataSpec[_, Root], conf: DerivationJobConf): Unit = {
    filterEnrichSave(ArchiveSpark.load(spec), conf)
  }

  def warcSpec(rdd: RDD[WarcRecord]): DataSpec[_, Root]

  def fileSpec(rdd: RDD[FileRecord]): DataSpec[_, Root]

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { sc =>
      SparkJobManager.initThread(sc, this, conf)
      conf.inputSpec.inputType match {
        case InputSpec.InputType.Files =>
          InputSpecLoader.load(conf.inputSpec) { rdd =>
            IOHelper.sample(rdd, conf.sample) { sample =>
              loadFilterEnrichSave(fileSpec(sample), conf)
              true
            }
          }
        case InputSpec.InputType.WARC =>
          WebArchiveLoader.loadWarcs(conf.inputSpec) { rdd =>
            IOHelper.sample(rdd, conf.sample) { sample =>
              loadFilterEnrichSave(warcSpec(sample), conf)
              true
            }
          }
        case _ =>
          throw new UnsupportedOperationException()
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
      DerivativeOutput(name.stripPrefix("/"), path, "ArchiveSpark", "application/gzip")
    }

  override val templateName: Option[String] = Some("jobs/DefaultArsJob")

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    super.templateVariables(conf) ++ JobManager.getInstance(this.id, conf).toSeq.flatMap {
      instance =>
        val wasapiUrl = ArchConf.baseUrl + {
          if (ArchConf.uuidJobOutPath.exists(instance.outPath.startsWith)) {
            "/api/job/" + instance.uuid + "/files"
          } else {
            "/wasapi/v1/jobs/" + id + "/result?collection=" + conf.inputSpec.id + {
              if (conf.isSample) "&sample=true" else ""
            }
          }
        }
        Seq(
          "wasapiUrl" -> wasapiUrl,
          "wasapiPages" -> (outFiles(
            conf).size.toDouble / WasapiController.FixedPageSize).ceil.toInt)
    }
  }
}

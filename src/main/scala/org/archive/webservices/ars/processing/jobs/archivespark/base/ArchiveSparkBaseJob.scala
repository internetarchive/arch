package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.ArchiveSpark
import org.archive.webservices.archivespark.dataspecs.DataSpec
import org.archive.webservices.ars.io.{IOHelper, WebArchiveLoader}
import org.archive.webservices.ars.model.DerivativeOutput
import org.archive.webservices.ars.model.collections.inputspecs.{FileRecord, InputSpec, InputSpecLoader}
import org.archive.webservices.ars.processing._
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.warc.WarcRecord

import scala.concurrent.Future

abstract class ArchiveSparkBaseJob extends ChainedJob {
  override def id: String = "ArchiveSpark" + super.id

  val relativeOutPath = s"/$id"
  val resultDir = "/out.json.gz"
  val resultFile = "/result.json.gz"

  lazy val children: Seq[PartialDerivationJob] = Seq(ArchiveSparkProcessor, PostProcessor)

  def enrich(rdd: RDD[ArchEnrichRoot[_]], conf: DerivationJobConf): RDD[ArchEnrichRoot[_]]

  def enrichSave(rdd: RDD[ArchEnrichRoot[_]], conf: DerivationJobConf): Unit = {
    enrich(rdd, conf).saveAsJson(conf.outputPath + relativeOutPath + resultDir)
  }

  def warcSpec(rdd: RDD[WarcRecord]): DataSpec[_, ArchWarcRecord] = ArchWarcSpec(rdd)

  def fileSpec(rdd: RDD[FileRecord]): DataSpec[_, ArchFileRecord] = ArchFileSpec(rdd)

  override val templateName: Option[String] = Some("jobs/DefaultArsJob")

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)

  def filePredicate(conf: DerivationJobConf): ArchFileRecord => Boolean = genericPredicate(conf)

  def warcPredicate(conf: DerivationJobConf): ArchWarcRecord => Boolean = genericPredicate(conf)

  def genericPredicate(conf: DerivationJobConf): ArchEnrichRoot[_] => Boolean = _ => true

  object ArchiveSparkProcessor extends PartialDerivationJob(this) with SparkJob {
    override val stage: String = "ArchiveSpark"

    def run(conf: DerivationJobConf): Future[Boolean] = {
      SparkJobManager.context.map { sc =>
        SparkJobManager.initThread(sc, ArchiveSparkBaseJob.this, conf)
        conf.inputSpec.inputType match {
          case InputSpec.InputType.Files =>
            val filter = filePredicate(conf)
            InputSpecLoader.loadSpark(conf.inputSpec) { rdd =>
              val asRdd = ArchiveSpark.load(fileSpec(rdd))
              IOHelper.sample(asRdd, conf.sample, samplingConditions = Seq(filter)) { sample =>
                val filtered = sample.filter(filter)
                enrichSave(filtered.map(_.asInstanceOf[ArchEnrichRoot[_]]), conf)
                true
              }
            }
          case t if InputSpec.InputType.warc(t) =>
            val filter = warcPredicate(conf)
            WebArchiveLoader.loadWarcs(conf.inputSpec) { rdd =>
              val asRdd = ArchiveSpark.load(warcSpec(rdd))
              IOHelper.sample(asRdd, conf.sample, samplingConditions = Seq(filter)) { sample =>
                val filtered = sample.filter(filter)
                enrichSave(filtered.map(_.asInstanceOf[ArchEnrichRoot[_]]), conf)
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
  }

  object PostProcessor extends PartialDerivationJob(this) with GenericJob {
    override val stage: String = "Post-Processing"

    def run(conf: DerivationJobConf): Future[Boolean] = Future {
      val outDir = conf.outputPath + relativeOutPath + resultDir
      val outFile = conf.outputPath + relativeOutPath + resultFile
      IOHelper.concatHdfs(
        outDir,
        outFile,
        _.endsWith(".gz"),
        decompress = false,
        deleteSrcFiles = true,
        deleteSrcPath = true) { in =>
        DerivativeOutput.hashFile(in, outFile)
      }
      HdfsIO.exists(outFile)
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      val outDir = conf.outputPath + relativeOutPath + resultDir
      val outFile = conf.outputPath + relativeOutPath + resultFile
      if (HdfsIO.exists(outFile)) {
        if (!HdfsIO.exists(outDir)) instance.state = ProcessingState.Finished
        else instance.state = ProcessingState.Failed
      }
      instance
    }

    override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
      val size = HdfsIO
        .files(conf.outputPath + relativeOutPath + resultFile)
        .map(HdfsIO.length)
        .sum
      Seq("resultSize" -> size)
    }

    override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] =
      Iterator(
        DerivativeOutput(
          resultFile.stripPrefix("/"),
          conf.outputPath + relativeOutPath,
          "ArchiveSpark/jsonl",
          "application/gzip"))
  }
}

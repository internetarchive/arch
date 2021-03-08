package org.archive.webservices.ars.processing.jobs.shared

import io.archivesunleashed.ArchiveRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.helge.sparkling.warc.WarcLoader
import org.archive.webservices.ars.io.{AutRecordLoader, IOHelper}
import org.archive.webservices.ars.processing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

abstract class AutJob extends ChainedJob {
  val relativeOutPath = s"/$id"

  lazy val children: Seq[PartialDerivationJob] = Seq(Spark, PostProcessor)

  def targetFile: String

  def df(rdd: RDD[ArchiveRecord]): Dataset[Row]

  def runSpark(rdd: RDD[ArchiveRecord], conf: DerivationJobConf): Unit = {
    df(rdd).write
      .option("timestampFormat", "yyyy/MM/dd HH:mm:ss ZZ")
      .format("csv")
      .csv(conf.outputPath + relativeOutPath)
  }

  def postProcess(outPath: String): Boolean = {
    IOHelper.concatLocal(
      outPath,
      targetFile,
      _.startsWith("part-"),
      compress = true,
      deleteSrcFiles = true) { tmpFile =>
      val outFile = outPath + "/" + targetFile
      HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
      HdfsIO.exists(outFile)
    }
  }

  object Spark extends PartialDerivationJob(this) with SparkJob {
    def succeeded(conf: DerivationJobConf): Boolean =
      HdfsIO.exists(conf.outputPath + relativeOutPath + "/_SUCCESS")

    def run(conf: DerivationJobConf): Future[Boolean] = {
      SparkJobManager.context.map { _ =>
        val rdd = IOHelper
          .load(
            conf.inputPath,
            RddUtil
              .loadBinary(_, decompress = false, close = false) { (filename, in) =>
                IteratorUtil.cleanup(
                  WarcLoader
                    .load(in)
                    .map(AutRecordLoader.fromWarc(filename, _, bufferBytes = true)),
                  in.close)
              },
            conf.sample)
        runSpark(rdd, conf)
        succeeded(conf)
      }
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      val started = HdfsIO.exists(conf.outputPath + relativeOutPath)
      if (started) {
        val completed = succeeded(conf)
        instance.state = if (completed) ProcessingState.Finished else ProcessingState.Failed
      }
      instance
    }
  }

  object PostProcessor extends PartialDerivationJob(this) with GenericJob {
    def run(conf: DerivationJobConf): Future[Boolean] = Future {
      Try(postProcess(conf.outputPath + relativeOutPath)).getOrElse(false)
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      val outPath = conf.outputPath + relativeOutPath
      if (HdfsIO.exists(outPath + "/" + targetFile)) {
        instance.state =
          if (HdfsIO.files(outPath + "/part-*").isEmpty) ProcessingState.Finished
          else ProcessingState.Failed
      }
      instance
    }

    override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
      val size = HdfsIO
        .files(conf.outputPath + relativeOutPath + "/" + targetFile)
        .map(HdfsIO.length)
        .sum
      Seq("resultSize" -> size)
    }
  }

  override def templateName: Option[String] = Some("jobs/DefaultAutJob")
}

package org.archive.webservices.ars.processing.jobs.shared

import io.archivesunleashed.ArchiveRecord
import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.helge.sparkling.warc.WarcLoader
import org.archive.webservices.ars.io.AutRecordLoader
import org.archive.webservices.ars.processing._
import org.apache.spark.sql._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

abstract class AutJob extends ChainedJob {
  val relativeOutPath = s"/$id"

  lazy val children: Seq[PartialDerivationJob] = Seq(Spark, PostProcessor)

  def targetFile: String

  def df(rdd: RDD[ArchiveRecord]): Dataset[Row]

  def runSpark(rdd: RDD[ArchiveRecord], outPath: String): Unit = {
    df(rdd).write
      .option("timestampFormat", "yyyy/MM/dd HH:mm:ss ZZ")
      .format("csv")
      .csv(outPath)
  }

  def postProcess(outPath: String): Boolean = {
    import sys.process._
    val outFile = targetFile.stripSuffix(".gz")
    Seq(
      "/bin/sh",
      "-c",
      "find " + outPath + " -iname 'part*' -type f -exec cat {} > " + outPath + "/" + outFile + " \\;").! == 0 &&
    Seq("/bin/sh", "-c", "gzip " + outPath + "/" + outFile).! == 0 &&
    Seq("/bin/sh", "-c", "find " + outPath + " -iname '*part*' -type f -delete").! == 0
  }

  object Spark extends PartialDerivationJob(this) with SparkJob {
    def succeeded(conf: DerivationJobConf): Boolean =
      HdfsIO.exists(conf.outputPath + relativeOutPath + "/_SUCCESS")

    def run(conf: DerivationJobConf): Future[Boolean] = {
      SparkJobManager.context.map { _ =>
        runSpark(
          RddUtil
            .loadBinary(conf.inputPath, decompress = false, close = false) { (filename, in) =>
              IteratorUtil.cleanup(
                WarcLoader
                  .load(in)
                  .map(AutRecordLoader.fromWarc(filename, _, bufferBytes = true)),
                in.close)
            },
          conf.outputPath + relativeOutPath)
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
          if (HdfsIO.files(outPath + "/*part*").isEmpty) ProcessingState.Finished
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

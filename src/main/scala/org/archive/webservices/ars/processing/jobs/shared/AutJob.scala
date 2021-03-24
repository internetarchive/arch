package org.archive.webservices.ars.processing.jobs.shared

import java.io.{OutputStream, PrintStream}

import io.archivesunleashed.ArchiveRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.helge.sparkling.warc.WarcLoader
import org.archive.webservices.ars.io.{AutRecordLoader, IOHelper}
import org.archive.webservices.ars.model.DerivativeOutput
import org.archive.webservices.ars.processing._

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
      .csv(outPath + "/_" + targetFile)
  }

  def checkSparkState(outPath: String): Option[Int] = {
    if (HdfsIO.exists(outPath + "/_" + targetFile)) Some {
      if (HdfsIO.exists(outPath + "/_" + targetFile + "/_SUCCESS")) ProcessingState.Finished
      else ProcessingState.Failed
    } else None
  }

  def prepareOutputStream(out: OutputStream): Unit =
    printToOutputStream(new PrintStream(out, true, "utf-8"))

  def printToOutputStream(out: PrintStream): Unit = {}

  def postProcess(outPath: String): Boolean = {
    IOHelper.concatLocal(
      outPath + "/_" + targetFile,
      targetFile,
      _.startsWith("part-"),
      compress = true,
      deleteSrcFiles = true,
      deleteSrcPath = true,
      prepare = prepareOutputStream) { tmpFile =>
      val outFile = outPath + "/" + targetFile
      HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
      HdfsIO.exists(outFile)
    }
  }

  def checkFinishedState(outPath: String): Option[Int] = {
    if (HdfsIO.exists(outPath + "/" + targetFile)) Some {
      if (HdfsIO.files(outPath + "/part-*").isEmpty) ProcessingState.Finished
      else ProcessingState.Failed
    } else None
  }

  object Spark extends PartialDerivationJob(this) with SparkJob {
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
                    .filter(r => r.isResponse || r.isRevisit)
                    .map(AutRecordLoader.fromWarc(filename, _, bufferBytes = true)),
                  in.close)
              },
            conf.sample)
        val outPath = conf.outputPath + relativeOutPath
        runSpark(rdd, outPath)
        checkSparkState(outPath).contains(ProcessingState.Finished)
      }
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      for (s <- checkSparkState(conf.outputPath + relativeOutPath)) instance.state = s
      instance
    }
  }

  object PostProcessor extends PartialDerivationJob(this) with GenericJob {
    def run(conf: DerivationJobConf): Future[Boolean] = Future {
      Try(postProcess(conf.outputPath + relativeOutPath)).getOrElse(false)
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      for (s <- checkFinishedState(conf.outputPath + relativeOutPath)) instance.state = s
      instance
    }

    override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
      val size = HdfsIO
        .files(conf.outputPath + relativeOutPath + "/" + targetFile)
        .map(HdfsIO.length)
        .sum
      Seq("resultSize" -> size)
    }

    override def outFiles(conf: DerivationJobConf): Seq[DerivativeOutput] =
      Seq(DerivativeOutput(targetFile, conf.outputPath + relativeOutPath, "application/gzip"))
  }

  override def templateName: Option[String] = Some("jobs/DefaultAutJob")
}

package org.archive.webservices.ars.processing.jobs.shared

import java.io.{OutputStream, PrintStream}

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.helge.sparkling.warc.{WarcLoader, WarcRecord}
import org.archive.webservices.ars.io.{CollectionLoader, IOHelper}
import org.archive.webservices.ars.model.DerivativeOutput
import org.archive.webservices.ars.processing._
import org.archive.helge.sparkling.Sparkling.executionContext
import org.archive.webservices.ars.aut.AutLoader

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

abstract class AutJob[R: ClassTag] extends ChainedJob {
  val relativeOutPath = s"/$id"

  lazy val children: Seq[PartialDerivationJob] = Seq(Spark, PostProcessor)

  def targetFile: String

  def prepareRecords(rdd: RDD[WarcRecord]): RDD[R]

  def df(rdd: RDD[R]): Dataset[Row]

  def runSpark(rdd: RDD[R], outPath: String): Unit =
    AutLoader.save(df(rdd), outPath + "/_" + targetFile)

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
      if (HdfsIO.exists(outFile)) {
        HdfsIO.writeLines(
          outFile + DerivativeOutput.lineCountFileSuffix,
          Seq((HdfsIO.countLines(outFile, copyLocal = false) - 1).toString))
        true
      } else {
        false
      }
    }
  }

  def checkFinishedState(outPath: String): Option[Int] = {
    if (HdfsIO.exists(outPath + "/" + targetFile)) Some {
      if (HdfsIO.files(outPath + "/part-*").isEmpty) ProcessingState.Finished
      else ProcessingState.Failed
    } else None
  }

  object Spark extends PartialDerivationJob(this) with SparkJob {
    override def name: String = "Processing"

    def run(conf: DerivationJobConf): Future[Boolean] = {
      SparkJobManager.context.map { _ =>
        val rdd = IOHelper
          .sample(
            prepareRecords(CollectionLoader.loadWarcs(conf.collectionId, conf.inputPath)),
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
    override def name: String = "Post-Processing"

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

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)
}

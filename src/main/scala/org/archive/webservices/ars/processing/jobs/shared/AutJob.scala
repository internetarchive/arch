package org.archive.webservices.ars.processing.jobs.shared

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.archive.webservices.ars.aut.AutLoader
import org.archive.webservices.ars.io.{CollectionLoader, IOHelper}
import org.archive.webservices.ars.model.DerivativeOutput
import org.archive.webservices.ars.processing._
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.compression.Gzip
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.{OutputStream, PrintStream}
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

abstract class AutJob[R: ClassTag] extends ChainedJob {
  val relativeOutPath = s"/$id"

  lazy val children: Seq[PartialDerivationJob] = Seq(Spark, PostProcessor)

  def targetFile: String

  def prepareRecords(rdd: RDD[WarcRecord]): RDD[R]

  def df(rdd: RDD[R]): Dataset[Row]

  def samplingConditions: Seq[R => Boolean] = Seq.empty

  def runSpark(rdd: RDD[R], outPath: String): Unit = {
    val data = AutLoader.saveAndLoad(df(rdd), outPath + "/_" + targetFile)

    HdfsIO.writeLines(
      outPath + "/" + targetFile + DerivativeOutput.LineCountFileSuffix,
      Seq(data.count.toString),
      overwrite = true)
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
    val outFile = outPath + "/" + targetFile
    IOHelper.concatHdfs(
      outPath + "/_" + targetFile,
      outFile,
      _.startsWith("part-"),
      decompress = false,
      deleteSrcFiles = true,
      deleteSrcPath = true,
      prepare = Gzip.compressOut(_)(prepareOutputStream)) { in =>
      DerivativeOutput.hashFile(in, outFile)
    }
    HdfsIO.exists(outFile)
  }

  def checkFinishedState(outPath: String): Option[Int] = {
    if (HdfsIO.exists(outPath + "/" + targetFile)) Some {
      if (HdfsIO.files(outPath + "/_*").isEmpty) ProcessingState.Finished
      else ProcessingState.Failed
    } else None
  }

  object Spark extends PartialDerivationJob(this) with SparkJob {
    def run(conf: DerivationJobConf): Future[Boolean] = {
      SparkJobManager.context.map { _ =>
        CollectionLoader.loadWarcs(conf.collectionId, conf.inputPath) { rdd =>
          IOHelper
            .sample(
              prepareRecords(rdd),
              conf.sample,
              samplingConditions) { rdd =>
              val outPath = conf.outputPath + relativeOutPath
              runSpark(rdd, outPath)
              checkSparkState(outPath).contains(ProcessingState.Finished)
            }
        }
      }
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      for (s <- checkSparkState(conf.outputPath + relativeOutPath)) instance.state = s
      instance
    }
  }

  object PostProcessor extends PartialDerivationJob(this) with GenericJob {
    override val stage: String = "Post-Processing"

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

    override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] =
      Iterator(
        DerivativeOutput(
          targetFile,
          conf.outputPath + relativeOutPath,
          "csv",
          "application/gzip"))
  }

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)
}

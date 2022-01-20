package org.archive.webservices.ars.processing.jobs

import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.util.RddUtil
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArchJobCategories, DerivativeOutput}
import org.archive.webservices.ars.processing._

import org.archive.webservices.sparkling.Sparkling.executionContext
import scala.concurrent.Future

object FileCountAndSize extends SparkJob {
  val name = "Count files and size"
  val category = ArchJobCategories.None
  def description = "Demo job."

  val relativeOutPath = s"/$id.gz"

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { _ =>
      val singlePartition =
        IOHelper
          .sample(
            RddUtil.loadFilesLocality(conf.inputPath, setPartitionFiles = false),
            conf.sample) { sample =>
            sample
              .map { file =>
                (true, (1L, HdfsIO.length(file)))
              }
              .reduceByKey({
                case ((c1, s1), (c2, s2)) =>
                  (c1 + c2, s1 + s2)
              }, numPartitions = 1)
          }
      val processed = RddUtil.saveAsTextFile(singlePartition.values.map {
        case (c, s) => c + "\t" + s
      }, conf.outputPath + relativeOutPath, skipIfExists = true, skipEmpty = false)

      processed > 0
    }
  }

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    val started = HdfsIO.exists(conf.outputPath + relativeOutPath)
    if (started) {
      val completed =
        HdfsIO.exists(conf.outputPath + relativeOutPath + "/" + Sparkling.CompleteFlagFile)
      instance.state = if (completed) ProcessingState.Finished else ProcessingState.Failed
    }
    instance
  }

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    val line = HdfsIO.lines(HdfsIO.files(conf.outputPath + relativeOutPath + "/*.gz").next).head
    val split = line.split("\t")
    Seq("resultFiles" -> split.head, "resultSize" -> split(1))
  }

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] =
    HdfsIO.files(conf.outputPath + relativeOutPath + "/*.gz").map { file =>
      val (path, name) = file.splitAt(file.lastIndexOf('/'))
      DerivativeOutput(name, path, "tsv", "application/gzip")
    }

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)
}

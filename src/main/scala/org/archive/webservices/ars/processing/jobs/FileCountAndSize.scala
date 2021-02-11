package org.archive.webservices.ars.processing.jobs

import org.archive.helge.sparkling.Sparkling
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.RddUtil
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FileCountAndSize extends SparkJob {
  val name = "Count files and size"
  val category = ArsCloudJobCategories.None
  def description = "Demo job."

  val relativeOutPath = s"/$id.gz"

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { _ =>
      val singlePartition = RddUtil
        .loadFilesLocality(conf.inputPath)
        .map { file =>
          (true, (1L, HdfsIO.length(file)))
        }
        .reduceByKey({
          case ((c1, s1), (c2, s2)) =>
            (c1 + c2, s1 + s2)
        }, numPartitions = 1)
      val rdd =
        if (conf.sample < 0) singlePartition
        else
          singlePartition.mapPartitionsWithIndex {
            case (idx, records) =>
              if (idx == 0) records.take(conf.sample) else Iterator.empty
          }
      val processed = RddUtil.saveAsTextFile(rdd.values.map {
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
}

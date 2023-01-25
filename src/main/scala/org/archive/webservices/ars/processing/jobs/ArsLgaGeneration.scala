package org.archive.webservices.ars.processing.jobs

import org.apache.spark.storage.StorageLevel
import org.archive.webservices.ars.io.{CollectionLoader, IOHelper}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.shared.ArsJob
import org.archive.webservices.ars.util.HttpUtil
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.ars.LGA
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.util.RddUtil

import scala.concurrent.Future
import scala.util.Try

object ArsLgaGeneration extends ChainedJob with ArsJob {
  val name = "Extract longitudinal graph"
  val category: ArchJobCategory = ArchJobCategories.Network
  def description =
    "Creates Longitudinal Graph Analysis (LGA) files which contain a complete list of what URLs link to what URLs, along with a timestamp."

  val relativeOutPath = s"/$id"
  val MapFile = "id.map.gz"
  val GraphFile = "id.graph.gz"

  override def children: Seq[PartialDerivationJob] = Seq(Spark, PostProcessor)

  object Spark extends PartialDerivationJob(this) with SparkJob {
    def run(conf: DerivationJobConf): Future[Boolean] = {
      SparkJobManager.context.map { _ =>
        IOHelper
          .sample({
            val warcs = CollectionLoader
              .loadWarcs(conf.collectionId, conf.inputPath)
              .filter(_.http.exists(http =>
                http.mime.contains("text/html") && http.status == 200))
            LGA.parse(warcs, http => Try(HttpUtil.bodyString(http.body, http)).getOrElse("")).filter(_._2.hasNext)
          }, conf.sample) { parsed =>
            val outPath = conf.outputPath + relativeOutPath
            val cached = parsed.persist(StorageLevel.DISK_ONLY)
            val processed = LGA.parsedToGraph(parsed) { mapRdd =>
              val path = outPath + "/_" + MapFile
              val strings = mapRdd.map(_.toJsonString)
              RddUtil.saveAsTextFile(strings, path)
              RddUtil.loadTextLines(path + "/*.gz", sorted = true).flatMap(LGA.parseNode)
            } { graphRdd =>
              val path = outPath + "/_" + GraphFile
              val strings = graphRdd.map(_.toJsonString)
              RddUtil.saveAsTextFile(strings, path)
            }
            cached.unpersist(true)
            processed >= 0
          }
      }
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      val started = HdfsIO.exists(conf.outputPath + relativeOutPath)
      if (started) {
        val completed =
          HdfsIO.exists(
            conf.outputPath + relativeOutPath + "/_" + MapFile + "/" + Sparkling.CompleteFlagFile) && HdfsIO
            .exists(
              conf.outputPath + relativeOutPath + "/_" + GraphFile + "/" + Sparkling.CompleteFlagFile)
        instance.state = if (completed) ProcessingState.Finished else ProcessingState.Failed
      }
      instance
    }
  }

  object PostProcessor extends PartialDerivationJob(this) with GenericJob {
    override val stage: String = "Post-Processing"

    def run(conf: DerivationJobConf): Future[Boolean] = Future {
      IOHelper.concatLocal(
        conf.outputPath + relativeOutPath + "/_" + MapFile,
        _.endsWith(".gz"),
        decompress = false,
        deleteSrcFiles = true,
        deleteSrcPath = true) { tmpFile =>
        val outFile = conf.outputPath + relativeOutPath + "/" + MapFile
        DerivativeOutput.hashFileLocal(tmpFile, outFile)
        HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
        HdfsIO.exists(outFile)
      } && IOHelper.concatLocal(
        conf.outputPath + relativeOutPath + "/_" + GraphFile,
        _.endsWith(".gz"),
        decompress = false,
        deleteSrcFiles = true,
        deleteSrcPath = true) { tmpFile =>
        val outFile = conf.outputPath + relativeOutPath + "/" + GraphFile
        DerivativeOutput.hashFileLocal(tmpFile, outFile)
        HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
        HdfsIO.exists(outFile)
      }
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      if (HdfsIO.exists(conf.outputPath + relativeOutPath + "/" + MapFile)) {
        instance.state =
          if (HdfsIO.exists(conf.outputPath + relativeOutPath + "/" + GraphFile) && HdfsIO
                .files(conf.outputPath + relativeOutPath + "/_*")
                .isEmpty)
            ProcessingState.Finished
          else ProcessingState.Failed
      }
      instance
    }

    override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] =
      Iterator(
        DerivativeOutput(
          MapFile,
          conf.outputPath + relativeOutPath,
          "lga-map",
          "application/gzip"),
        DerivativeOutput(
          GraphFile,
          conf.outputPath + relativeOutPath,
          "lga-graph",
          "application/gzip"))
  }

  override val templateName: Option[String] = Some("jobs/DefaultArsJob")

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)
}

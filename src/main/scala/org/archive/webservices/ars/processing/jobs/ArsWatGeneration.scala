package org.archive.webservices.ars.processing.jobs

import java.io.InputStream

import org.apache.hadoop.fs.Path
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.ars.WAT
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.util.{IteratorUtil, RddUtil, StringUtil}
import org.archive.webservices.ars.io.{CollectionLoader, IOHelper}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.shared.ArsJob

import scala.concurrent.Future

object ArsWatGeneration extends SparkJob with ArsJob {
  val name = "Generate web archive transformation (WAT) files"
  val category: ArchJobCategory = ArchJobCategories.Collection
  def description = "Creates Web Archive Transformation (WAT) files that include a brief header which identifies its corresponding URL via \"WARC-Target-URI,\" corresponding W/ARC file via \"WARC-Refers-To,\" and additional mapping information."

  val relativeOutPath = s"/$id"
  val resultDir = "/wat.gz"

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { _ =>
      IOHelper
        .sampleGrouped[String, InputStream, Boolean](
          CollectionLoader.loadWarcFiles(conf.collectionId, conf.inputPath).map {
            case (path, in) =>
              val file = new Path(path).getName
              val outFile = StringUtil.stripSuffix(file, Sparkling.GzipExt) + ".wat.gz"
              val watIn = WAT.fromWarcStream(in, file, Some(outFile), bubbleClose = true)
              (file, IteratorUtil.cleanup(GzipUtil.decompressConcatenated(watIn), watIn.close))
          },
          conf.sample) { rdd =>
          val outPath = conf.outputPath + relativeOutPath + resultDir
          val processed = RddUtil.saveGroupedAsNamedFiles(rdd.map {
            case (f, in) =>
              val outFile = StringUtil.stripSuffix(f, Sparkling.GzipExt) + ".wat.gz"
              (outFile, in)
          }, outPath, compress = true, skipIfExists = true)
          processed > 0
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
      DerivativeOutput(name.stripPrefix("/"), path, "wat", "application/gzip")
    }

  override val templateName: Option[String] = Some("jobs/DefaultArsJob")

  override def reset(conf: DerivationJobConf): Unit =
    HdfsIO.delete(conf.outputPath + relativeOutPath)
}

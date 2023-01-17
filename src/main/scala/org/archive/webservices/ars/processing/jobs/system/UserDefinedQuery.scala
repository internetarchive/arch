package org.archive.webservices.ars.processing.jobs.system

import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.io.CollectionLoader
import org.archive.webservices.ars.model.collections.CustomCollectionSpecifics
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.cdx.{CdxLoader, CdxUtil}
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.logging.LogContext
import org.archive.webservices.sparkling.util.RddUtil

import scala.concurrent.Future

object UserDefinedQuery extends SparkJob with DerivationJob {
  implicit val logContext: LogContext = LogContext(this)

  val name = "User-Defined Query"
  val category: ArchJobCategory = ArchJobCategories.System
  def description = "Job to run a user-defined query (internal system job)"

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { sc =>
      val paramsBc = sc.broadcast(conf.params)
      val cdx = CollectionLoader.loadWarcFiles(conf.collectionId, conf.inputPath).flatMap { case (path, in) =>
        CdxUtil.fromWarcGzStream(path, in)
      }.mapPartitions { partition =>
        val params = paramsBc.value
        partition.filter { cdx =>
          {
            params.get[String]("surtPrefix").forall(cdx.surtUrl.startsWith)
          } && {
            params.get[String]("timestampFrom").forall(cdx.timestamp >= _)
          } && {
            params.get[String]("timestampTo").forall(t => cdx.timestamp <= t || cdx.timestamp.startsWith(t))
          } && {
            params.get[Int]("status").forall(cdx.status == _)
          } && {
            params.get[String]("mime").forall(cdx.mime == _)
          }
        }
      }
      val cdxPath = conf.outputPath + "/" + CustomCollectionSpecifics.CdxDir
      RddUtil.saveAsTextFile(cdx.map(_.toCdxString), cdxPath, skipIfExists = true, checkPerFile = true, skipEmpty = true)
      if (HdfsIO.exists(cdxPath + "/" + Sparkling.CompleteFlagFile)) {
        val size = CdxLoader.load(cdxPath + "/*.cdx.gz").map(_.compressedSize).fold(0L)(_ + _)
        val info = conf.params.set("size" -> size)
        val infoPath = conf.outputPath + "/" + CustomCollectionSpecifics.InfoFile
        HdfsIO.writeLines(infoPath, Seq(info.toJson.spaces4))
        HdfsIO.exists(infoPath)
      } else false
    }
  }

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    val started = HdfsIO.exists(conf.outputPath)
    if (started) {
      val completed = HdfsIO.exists(conf.outputPath + "/" + CustomCollectionSpecifics.InfoFile)
      instance.state = if (completed) ProcessingState.Finished else ProcessingState.Failed
    }
    instance
  }

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] = Iterator.empty

  override val templateName: Option[String] = None

  override def reset(conf: DerivationJobConf): Unit = HdfsIO.delete(conf.outputPath)
}

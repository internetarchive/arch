package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.app.WebPagesExtractor
import org.archive.helge.sparkling.Sparkling
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.helge.sparkling.warc.WarcLoader
import org.archive.webservices.ars.io.AutRecordLoader
import org.archive.webservices.ars.processing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object WebPagesExtraction extends SparkJob {
  val id = "WebPagesExtraction"
  val name = "Extract webpages"
  val relativeOutPath = s"/$id"

  def succeeded(conf: DerivationJobConf): Boolean = HdfsIO.exists(conf.outputPath + relativeOutPath + "/_SUCCESS")

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkManager.context.map { _ =>
      val df = RddUtil.loadBinary(conf.inputPath, decompress = false, close = false) { (filename, in) =>
        IteratorUtil.cleanup(WarcLoader.load(in).map(AutRecordLoader.fromWarc(filename, _, bufferBytes = true)), in.close)
      }.webpages()
      WebPagesExtractor(df).write.option("timestampFormat", "yyyy/MM/dd HH:mm:ss ZZ").format("csv").csv(conf.outputPath + relativeOutPath)
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

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    val numFiles = HdfsIO.files(conf.outputPath + relativeOutPath + "/*.csv").size
    Seq(
      "numFiles" -> numFiles
    )
  }
}

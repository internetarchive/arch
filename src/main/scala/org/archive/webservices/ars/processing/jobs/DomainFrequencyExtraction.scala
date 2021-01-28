package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.app.DomainFrequencyExtractor
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.helge.sparkling.warc.WarcLoader
import org.archive.webservices.ars.io.AutRecordLoader
import org.archive.webservices.ars.processing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object DomainFrequencyExtraction extends ChainedJob {
  val id = "DomainFrequencyExtraction"
  val name = "Domain Frequency"
  val relativeOutPath = s"/$id"

  addJob(new PartialDerivationJob(_) with SparkJob {
    def succeeded(conf: DerivationJobConf): Boolean = HdfsIO.exists(conf.outputPath + relativeOutPath + "/_SUCCESS")

    def run(conf: DerivationJobConf): Future[Boolean] = {
      SparkJobManager.context.map { _ =>
        val df = RddUtil.loadBinary(conf.inputPath, decompress = false, close = false) { (filename, in) =>
          IteratorUtil.cleanup(WarcLoader.load(in).map(AutRecordLoader.fromWarc(filename, _, bufferBytes = true)), in.close)
        }.webpages()
        DomainFrequencyExtractor(df).write.option("timestampFormat", "yyyy/MM/dd HH:mm:ss ZZ").format("csv").csv(conf.outputPath + relativeOutPath)
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
  })

  addJob(new PartialDerivationJob(_) with GenericJob {
    def run(conf: DerivationJobConf): Future[Boolean] = Future {
      import sys.process._
      val outPath = conf.outputPath + relativeOutPath
      Try {
        Seq("/bin/sh", "-c", "find " + outPath + " -iname 'part*' -type f -exec cat {} > " + outPath + "/domain-frequency.csv \\;").! == 0
        Seq("/bin/sh", "-c", "gzip " + outPath + "/domain-frequency.csv").! == 0
        Seq("/bin/sh", "-c", "find " + outPath + " -iname '*part*' -type f -delete").! == 0
      }.getOrElse(false)
    }

    override def history(conf: DerivationJobConf): DerivationJobInstance = {
      val instance = super.history(conf)
      val outPath = conf.outputPath + relativeOutPath
      if (HdfsIO.exists(outPath + "/domain-frequency.tar.gz")) {
        instance.state = if (HdfsIO.files(outPath + "/*.csv.gz").isEmpty) ProcessingState.Finished else ProcessingState.Failed
      }
      instance
    }

    override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
      val size = HdfsIO.files(conf.outputPath + relativeOutPath + "/*.csv.gz").map(HdfsIO.length).sum
      Seq(
        "resultSize" -> size
      )
    }
  })
}

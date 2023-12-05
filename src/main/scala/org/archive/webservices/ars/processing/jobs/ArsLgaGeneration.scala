package org.archive.webservices.ars.processing.jobs

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
  val name = "Longitudinal graph"
  val uuid = "01895064-661c-79da-9ca7-cbf82507de61"
  val category: ArchJobCategory = ArchJobCategories.Network
  def description =
    "All links between URLs in the collection over time. Output: one compressed TGZ file containing two compressed GZ files with data in JSON format representing, 1) a unique ID and SURT for each URL, and 2) the source URL ID, target URL ID, and timestamp for each link."

  val relativeOutPath = s"/$id"
  val ParsedCacheFile = "parsed.gz"
  val MapFile = "id.map.gz"
  val GraphFile = "id.graph.gz"

  val MaxPartitions = 100

  override def children: Seq[PartialDerivationJob] = Seq(Spark, PostProcessor)

  implicit val parsedInOut = TypedInOut.toStringInOut[Option[((LGA.LgaLabel, String), Iterator[LGA.LgaLabel])]]({
    case Some(((src, ts), dsts)) =>
      (Seq(src.surt, src.url, ts) ++ dsts.flatMap { dst => Iterator(dst.surt, dst.url)}).mkString("\t")
    case None => ""
  }, { str =>
    val split = str.split("\t")
    if (split.length > 3) Some {
      ((LGA.LgaLabel(split(0), split(1)), split(2)), split.drop(3).grouped(2).filter(_.length == 2).map { dst =>
        LGA.LgaLabel(dst(0), dst(1))
      })
    } else None
  })

  object Spark extends PartialDerivationJob(this) with SparkJob {
    def run(conf: DerivationJobConf): Future[Boolean] = {
      SparkJobManager.context.map { sc =>
        SparkJobManager.initThread(sc, ArsLgaGeneration, conf)
        CollectionLoader
          .loadWarcs(conf.collectionId, conf.inputPath) { rdd =>
            IOHelper
              .sample(
                {
                  val warcs = rdd
                    .filter(_.http.exists(http =>
                      http.mime.contains("text/html") && http.status == 200))
                  LGA
                    .parse(warcs, http => Try(HttpUtil.bodyString(http.body, http)).getOrElse(""))
                    .filter(_._2.hasNext)
                },
                conf.sample) { parsed =>
                val outPath = conf.outputPath + relativeOutPath
                val cachePath = outPath + "/" + ParsedCacheFile
                RddUtil.saveTyped(parsed.coalesce(MaxPartitions).map(Option(_)), cachePath)
                try {
                  val cached = RddUtil.loadTyped(cachePath + "/*.gz").flatMap(opt => opt)
                  LGA.parsedToBigGraph(cached) { mapRdd =>
                    val path = outPath + "/_" + MapFile
                    RddUtil.saveAsTextFile(mapRdd.map(_.toJsonString), path)
                    RddUtil.loadTextLines(path + "/*.gz", sorted = true).flatMap(LGA.parseNode)
                  } { graphRdd =>
                    val path = outPath + "/_" + GraphFile
                    val strings = graphRdd.map(_.toJsonString)
                    RddUtil.saveAsTextFile(strings, path) >= 0
                  }
                } finally {
                  HdfsIO.delete(cachePath)
                }
              }
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

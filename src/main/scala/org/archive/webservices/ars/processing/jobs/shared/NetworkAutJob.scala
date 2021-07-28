package org.archive.webservices.ars.processing.jobs.shared

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.apache.spark.storage.StorageLevel
import org.archive.helge.sparkling.Sparkling
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{RddUtil, SurtUtil}
import org.archive.webservices.ars.aut.AutLoader
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.{DerivationJobConf, ProcessingState}
import org.archive.webservices.ars.util.Common

import scala.reflect.ClassTag

abstract class NetworkAutJob[R: ClassTag] extends AutJob[R] {
  val SampleTopNNodes = 50

  val category: ArchJobCategory = ArchJobCategories.Network

  val sampleGraphFile: String = "sample-graph.tsv.gz"

  def srcDstFields: (String, String)

  def edgeCounts(df: Dataset[Row]): RDD[((String, String), Long)] = {
    val (srcField, dstField) = srcDstFields
    df.rdd
      .flatMap { row =>
        Common.tryOrElse[Option[((String, String), Long)]](None) {
          val src = row.getAs[String](srcField)
          val dst = row.getAs[String](dstField)
          val srcHost = SurtUtil.validateHost(SurtUtil.fromUrl(src))
          val dstHost = SurtUtil.validateHost(SurtUtil.fromUrl(dst))
          if (srcHost.isDefined && dstHost.isDefined && srcHost.get != dstHost.get) {
            Some(((srcHost.get, dstHost.get), 1L))
          } else None
        }
      }
      .reduceByKey(_ + _)
  }

  def createVizSample[O](df: Dataset[Row])(action: RDD[(String, String)] => O): O = {
    val hostEdges = edgeCounts(df).persist(StorageLevel.MEMORY_AND_DISK_SER)

    try {
      val nodes = hostEdges
        .flatMap {
          case ((src, dst), count) =>
            Iterator((src, count), (dst, count))
        }
        .reduceByKey(_ + _)
        .sortBy(-_._2)
        .take(SampleTopNNodes)
        .map(_._1)
        .toSet

      val nodesBc = hostEdges.context.broadcast(nodes)
      action(hostEdges.mapPartitions { partition =>
        val nodes = nodesBc.value
        partition.map(_._1).filter {
          case (src, dst) =>
            nodes.contains(src) && nodes.contains(dst)
        }
      })
    } finally {
      hostEdges.unpersist(true)
    }
  }

  override def runSpark(rdd: RDD[R], outPath: String): Unit = {
    val data = AutLoader.saveAndLoad(df(rdd), outPath + "/_" + targetFile)

    createVizSample(data) { derivative =>
      RddUtil.saveAsTextFile(
        derivative.map { case (s, d) => s"$s\t$d" },
        outPath + "/_" + sampleGraphFile)
    }
  }

  override def checkSparkState(outPath: String): Option[Int] =
    super.checkSparkState(outPath).map { state =>
      if (!HdfsIO.exists(outPath + "/_" + sampleGraphFile + "/" + Sparkling.CompleteFlagFile))
        ProcessingState.Failed
      else state
    }

  override def postProcess(outPath: String): Boolean = super.postProcess(outPath) && {
    IOHelper.concatLocal(
      outPath + "/_" + sampleGraphFile,
      sampleGraphFile,
      _.endsWith(".tsv.gz"),
      compress = true,
      deleteSrcFiles = true,
      deleteSrcPath = true) { tmpFile =>
      val outFile = outPath + "/" + sampleGraphFile
      HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
      HdfsIO.exists(outFile)
    }
  }

  override def checkFinishedState(outPath: String): Option[Int] =
    super.checkFinishedState(outPath).map { state =>
      if (!HdfsIO.exists(outPath + "/" + sampleGraphFile)) ProcessingState.Failed
      else state
    }

  override def templateName: Option[String] = Some("jobs/NetworkExtraction")

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    val edges = HdfsIO
      .lines(conf.outputPath + relativeOutPath + "/" + sampleGraphFile)
      .map(_.split("\t", 2))
      .map {
        case Array(src, dst) =>
          (src, dst)
      }

    val nodes =
      edges.flatMap { case (src, dst) => Iterator(src, dst) }.distinct.sorted.zipWithIndex
    val nodeMap = nodes.toMap

    super.templateVariables(conf) ++ Seq("nodes" -> nodes.map {
      case (node, id) => (node.split(',').reverse.mkString("."), id)
    }, "edges" -> edges.map { case (src, dst) => (nodeMap(src), nodeMap(dst)) })
  }
}

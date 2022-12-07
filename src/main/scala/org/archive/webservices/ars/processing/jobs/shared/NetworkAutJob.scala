package org.archive.webservices.ars.processing.jobs.shared

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Row}
import org.apache.spark.storage.StorageLevel
import org.archive.webservices.ars.aut.AutLoader
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing.{DerivationJobConf, ProcessingState}
import org.archive.webservices.ars.util.Common
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.{CollectionUtil, SurtUtil}

import scala.reflect.ClassTag

abstract class NetworkAutJob[R: ClassTag] extends AutJob[R] {
  val SampleTopNNodes = 50
  val SampleTopNEdges = 100

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

  def createVizSample[O](df: Dataset[Row])(action: Seq[(String, String)] => O): O = {
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
      val nodeRanks = nodes.zipWithIndex.toMap
      val nodesBc = hostEdges.context.broadcast(nodes.toSet)
      val edges = hostEdges
        .mapPartitions { partition =>
          val nodes = nodesBc.value
          partition.map(_._1).filter {
            case (src, dst) =>
              nodes.contains(src) && nodes.contains(dst)
          }
        }
        .collect
        .toSeq
        .groupBy { case (src, dst) => if (nodeRanks(src) < nodeRanks(dst)) src else dst }
      nodesBc.destroy()
      var available = Map.empty[String, Seq[(String, String)]]
      var sampleSize = 0
      val sample = nodes.toIterator.flatMap { node =>
        if (sampleSize < SampleTopNEdges) {
          available =
            CollectionUtil.combineMaps(available, edges.getOrElse(node, Seq.empty).groupBy {
              case (src, dst) =>
                if (src == node) dst else src
            })(values => Some(values.reduce(_ ++ _)))
          val next = available.getOrElse(node, Seq.empty)
          sampleSize += next.size
          next
        } else Seq.empty
      }.toSeq
      action(sample)
    } finally {
      hostEdges.unpersist(true)
    }
  }

  override def runSpark(rdd: RDD[R], outPath: String): Unit = {
    val data = AutLoader.saveAndLoad(df(rdd), outPath + "/_" + targetFile)

    HdfsIO.writeLines(
      outPath + "/" + targetFile + DerivativeOutput.LineCountFileSuffix,
      Seq(data.count.toString),
      overwrite = true)

    createVizSample(data) { derivative =>
      HdfsIO.writeLines(outPath + "/" + sampleGraphFile, derivative.map {
        case (s, d) => s"$s\t$d"
      })
    }
  }

  override def checkSparkState(outPath: String): Option[Int] =
    super.checkSparkState(outPath).map { state =>
      if (!HdfsIO.exists(outPath + "/" + sampleGraphFile)) ProcessingState.Failed
      else state
    }

  override def checkFinishedState(outPath: String): Option[Int] =
    super.checkFinishedState(outPath).map { state =>
      if (!HdfsIO.exists(outPath + "/" + sampleGraphFile)) ProcessingState.Failed
      else state
    }

  override val templateName: Option[String] = Some("jobs/NetworkExtraction")

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    val edges = HdfsIO
      .lines(
        conf.outputPath + relativeOutPath + "/" + sampleGraphFile,
        n = SampleTopNEdges + SampleTopNNodes)
      .map(_.split('\t'))
      .filter(_.length == 2)
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

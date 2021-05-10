package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.matchbox.ExtractLinks
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.desc
import org.apache.spark.sql.{Dataset, Row}
import org.apache.spark.storage.StorageLevel
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.aut.{AutLoader, AutUtil}
import org.archive.webservices.ars.processing.jobs.shared.NetworkAutJob
import org.archive.webservices.ars.util.Common

object DomainGraphExtraction extends NetworkAutJob[((String, String, String), Long)] {
  val name = "Extract domain graph"
  val description =
    "Create a CSV with the following columns: crawl date, source domain, destination domain, and count."

  val targetFile: String = "domain-graph.csv.gz"

  val srcDstFields: (String, String) = ("src_domain", "dest_domain")

  override def printToOutputStream(out: PrintStream): Unit =
    out.println("crawl_date,source,destination,count")

  override def df(rdd: RDD[((String, String, String), Long)]): Dataset[Row] = {
    val rows =
      rdd
        .reduceByKey(_ + _)
        .filter(_._2 > 5)
        .map {
          case ((date, source, target), count) =>
            Row(date, source, target, count)
        }
    AutLoader.domainGraph(rows).orderBy(desc("count"))
  }

  override def prepareRecords(rdd: RDD[WarcRecord]): RDD[((String, String, String), Long)] = {
    rdd
      .flatMap { r =>
        r.http.filter(AutUtil.validPage(r, _)).toIterator.flatMap { http =>
          Common
            .tryOrElse(Seq.empty[((String, String, String), Long)]) {
              val url = AutUtil.url(r)
              ExtractLinks(url, http.bodyString)
                .map {
                  case (source, target, _) =>
                    (
                      AutUtil.extractDomainRemovePrefixWWW(source),
                      AutUtil.extractDomainRemovePrefixWWW(target))
                }
                .filter { case (s, t) => s != "" && t != "" }
                .map {
                  case (source, target) =>
                    ((AutUtil.crawlDate(r), source, target), 1L)
                }
            }
            .toIterator
        }
      }
  }

  override def createVizSample[O](df: Dataset[Row])(action: RDD[(String, String)] => O): O = {
    val (srcField, dstField) = srcDstFields

    val hostEdges = df.rdd
      .map { row =>
        (row.getAs[String](srcField), row.getAs[String](dstField), row.getAs[Long]("count"))
      }
      .persist(StorageLevel.MEMORY_AND_DISK_SER)

    val nodes = hostEdges
      .flatMap {
        case (src, dst, count) =>
          Iterator((src, count), (dst, count))
      }
      .reduceByKey(_ + _)
      .sortBy(-_._2)
      .take(SampleTopNNodes)
      .map(_._1)
      .toSet

    val nodesBc = hostEdges.context.broadcast(nodes)

    try {
      action(hostEdges.map { case (src, dst, _) => (src, dst) }.mapPartitions { partition =>
        val nodes = nodesBc.value
        partition.filter {
          case (src, dst) =>
            nodes.contains(src) && nodes.contains(dst)
        }
      })
    } finally {
      hostEdges.unpersist(true)
    }
  }
}

package org.archive.webservices.ars.io

import java.io._
import java.nio.file.Files

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.hadoop.fs.Path
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{IteratorUtil, RddUtil}
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.util.FormatUtil

import scala.reflect.ClassTag
import scala.util.Try

object IOHelper {
  val SamplingScaleUpFactor = 4 // see RDD#take (conf.getInt("spark.rdd.limit.scaleUpFactor", 4))
  val SamplingMaxReadPerPartitionFactor = 2

  def tempDir[R](action: String => R): R = {
    val tmpPath = new File(ArchConf.localTempPath)
    tmpPath.mkdirs()
    val dir = Files.createTempDirectory(tmpPath.toPath, "ars-")
    try {
      action(dir.toString)
    } finally {
      Try(FileUtils.deleteDirectory(dir.toFile))
    }
  }

  def concatLocal[R](
      srcPath: String,
      dstFile: String,
      filter: String => Boolean = _ => true,
      compress: Boolean = false,
      deleteSrcFiles: Boolean = false,
      deleteSrcPath: Boolean = false,
      prepare: OutputStream => Unit = _ => {})(action: String => R): R = {
    val srcFiles =
      HdfsIO.files(srcPath).filter(_.split('/').lastOption.exists(filter)).toSeq.sorted
    IOHelper.tempDir { dir =>
      val tmpOutFile = dir + "/" + dstFile
      val out = new BufferedOutputStream(new FileOutputStream(tmpOutFile))
      val compressed = if (compress) new GzipCompressorOutputStream(out) else out
      prepare(compressed)
      try {
        for (file <- srcFiles) HdfsIO.access(file)(IOUtils.copy(_, compressed))
      } finally {
        compressed.close()
      }
      val r = action(tmpOutFile)
      if (deleteSrcFiles) for (file <- srcFiles) HdfsIO.delete(file)
      if (deleteSrcPath) HdfsIO.delete(srcPath)
      r
    }
  }

  def sample[F: ClassTag](
      rdd: RDD[F],
      sample: Int = -1,
      samplingConditions: Seq[F => Boolean] = Seq.empty): RDD[F] = {
    if (sample < 0) rdd
    else {
      val sc = rdd.sparkContext
      val conditions = if (samplingConditions.isEmpty) Seq((_: F) => true) else samplingConditions
      val conditionsBc = sc.broadcast(conditions)
      val maxPartitions = rdd.getNumPartitions
      var start = 0
      var totalResults = Seq.empty[(Int, Set[Int], Int)]
      val partitions = IteratorUtil
        .last {
          Iterator
            .continually(true)
            .zipWithIndex
            .map(_._2)
            .map(Math.pow(SamplingScaleUpFactor, _).toInt)
            .flatMap { numPartitions =>
              val end = start + numPartitions
              val partitions = (start until maxPartitions.min(end)).toList
              val results =
                sc.runJob(
                  rdd,
                  (context: TaskContext, partition: Iterator[F]) => {
                    var read = 0
                    var take = 0
                    (context.partitionId, {
                      val conditions = conditionsBc.value.zipWithIndex
                      val candidates = partition.take(sample * SamplingMaxReadPerPartitionFactor)
                      var matches = Set.empty[Int]
                      while (matches.size < conditions.size && candidates.hasNext) {
                        val r = candidates.next
                        read += 1
                        val matching = conditions.filter(_._1(r)).map(_._2).toSet
                        if ((matching -- matches).nonEmpty) {
                          matches ++= matching
                          take = read
                        }
                      }
                      matches
                    }, take)
                  },
                  partitions)
              var matches = Set.empty[Int]
              totalResults = (totalResults ++ results).sortBy(-_._2.size)
              val matchingPartitions = totalResults.flatMap {
                case (p, c, t) =>
                  if ((c -- matches).nonEmpty) {
                    matches ++= c
                    Some((p, t))
                  } else None
              }.sorted
              val continue = end < maxPartitions && matches.size < conditions.size
              if (continue) {
                start = start + numPartitions
                Iterator(Some(matchingPartitions))
              } else Iterator(Some(matchingPartitions), None)
            }
            .takeWhile(_.isDefined)
            .flatten
        }
        .toSet
        .flatten
      val numRecords = partitions.map(_._2).sum
      val takeMap = partitions.toMap
      val firstIdx = takeMap.keySet.min
      val requiredBc = sc.broadcast(
        takeMap.updated(
          firstIdx,
          takeMap(firstIdx) + (if (numRecords < sample) sample - numRecords else 0)))
      rdd.mapPartitionsWithIndex { (idx, p) =>
        val take = requiredBc.value
        if (take.keySet.contains(idx)) p.take(take(idx)) else Iterator.empty
      }
    }
  }

  def size(path: String): Long = HdfsIO.files(path).map(HdfsIO.length).sum

  def sizeStr(path: String): String = FormatUtil.formatBytes(size(path))

  def syncHdfs[R](path: String)(action: => R): R = {
    val p = new Path(path)
    var out: Option[OutputStream] = None
    var exists: Boolean = true
    while (out.isEmpty) {
      try {
        out = Some(if (exists) HdfsIO.fs.append(p) else HdfsIO.fs.create(p, false))
      } catch {
        case _: FileNotFoundException =>
          exists = false
        case _: Exception =>
          exists = true
          Thread.sleep(1000)
      }
    }
    try {
      action
    } finally {
      out.get.close()
      Try(HdfsIO.delete(path))
    }
  }
}

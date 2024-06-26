package org.archive.webservices.ars.io

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.hadoop.fs.Path
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.archive.webservices.ars.util.FormatUtil
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.util.{CleanupIterator, IteratorUtil}

import java.io._
import java.nio.file.Files
import java.time.Instant
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.util.Try

object IOHelper {
  val SamplingScaleUpFactor = 4 // see RDD#take (conf.getInt("spark.rdd.limit.scaleUpFactor", 4))
  val SamplingMaxReadPerPartitionFactor = 2
  val SpecialCharEscape = "-"

  def escapePath(path: String): String = {
    path
      .replace(ArchCollection.UserIdSeparator, SpecialCharEscape)
      .replace(ArchUser.PrefixNameSeparator, SpecialCharEscape)
  }

  def pathTimestamp(timestamp: Instant): String =
    timestamp.toString.replaceAll("[^\\d]", "").take(14)

  def concatPaths(paths: String*): String = {
    paths.map(_.trim).filter(_.nonEmpty).mkString("/").replaceAll("/+", "/")
  }

  def tempDir[R](action: String => R): R = {
    val tmpPath = new File(ArchConf.localTempPath)
    tmpPath.mkdirs()
    val dir = Files.createTempDirectory(tmpPath.toPath, "arch-")
    try {
      action(dir.toString)
    } finally {
      Try(FileUtils.deleteDirectory(dir.toFile))
    }
  }

  def concatLocal[R](
      srcPath: String,
      filter: String => Boolean = _ => true,
      decompress: Boolean = true,
      compress: Boolean = false,
      deleteSrcFiles: Boolean = false,
      deleteSrcPath: Boolean = false,
      prepare: OutputStream => Unit = _ => {})(action: String => R): R = {
    val srcFiles =
      HdfsIO.files(srcPath).filter(_.split('/').lastOption.exists(filter)).toSeq.sorted
    IOHelper.tempDir { dir =>
      val tmpOutFile = dir + "/concat.out"
      val out = new BufferedOutputStream(new FileOutputStream(tmpOutFile))
      val compressed = if (compress) new GzipCompressorOutputStream(out) else out
      prepare(compressed)
      try {
        for (file <- srcFiles)
          HdfsIO.access(file, decompress = decompress)(IOUtils.copy(_, compressed))
      } finally {
        compressed.close()
      }
      val r = action(tmpOutFile)
      if (deleteSrcFiles) for (file <- srcFiles) HdfsIO.delete(file)
      if (deleteSrcPath) HdfsIO.delete(srcPath)
      r
    }
  }

  def concatHdfs[R](
      srcPath: String,
      dstPath: String,
      filter: String => Boolean = _ => true,
      decompress: Boolean = true,
      compress: Boolean = false,
      deleteSrcFiles: Boolean = false,
      deleteSrcPath: Boolean = false,
      useWriter: Boolean = false,
      prepare: OutputStream => Unit = _ => {})(action: InputStream => R): R = {
    val srcFiles =
      HdfsIO.files(srcPath).filter(_.split('/').lastOption.exists(filter)).toSeq.sorted
    val tmpOutFile = dstPath + "_concatenating"

    val in = new ChainedInputStream(
      srcFiles.toIterator.map(HdfsIO.open(_, decompress = decompress)))
    val outIn = InOutInputStream(in) { out =>
      val compressed = if (compress) new GzipCompressorOutputStream(out) else out
      prepare(compressed)
      compressed
    }

    val out = HdfsIO.out(tmpOutFile, compress = false, useWriter = false)

    val forker = InputStreamForker(outIn)
    val Array(writeIn, forkIn) = forker.fork(2).map(Future(_))

    val r =
      try {
        Await.result(
          Future.sequence(Seq(writeIn.map(IOUtils.copy(_, out)), forkIn.map(action))),
          Duration.Inf)
      } finally {
        for (s <- writeIn) Try(s.close())
        for (s <- forkIn) Try(s.close())
        Try(outIn.close())
        Try(out.close())
      }
    HdfsIO.rename(tmpOutFile, dstPath)

    if (deleteSrcFiles) for (file <- srcFiles) HdfsIO.delete(file)
    if (deleteSrcPath) HdfsIO.delete(srcPath)

    r.last.asInstanceOf[R]
  }

  def sample[F: ClassTag, R](
      rdd: RDD[F],
      sample: Int = -1,
      samplingConditions: Seq[F => Boolean] = Seq.empty)(action: RDD[F] => R): R =
    sampleGrouped(
      rdd.mapPartitions(p => Iterator((true, CleanupIterator(p)))),
      sample,
      samplingConditions)(rdd => action(rdd.flatMap(_._2)))

  def sampleGrouped[K: ClassTag, F: ClassTag, R](
      rdd: RDD[(K, CleanupIterator[F])],
      sample: Int = -1,
      samplingConditions: Seq[F => Boolean] = Seq.empty)(
      action: RDD[(K, CleanupIterator[F])] => R): R = {
    if (sample < 0) action(rdd)
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
                  (context: TaskContext, partition: Iterator[(K, CleanupIterator[F])]) => {
                    var read = 0
                    var take = 0
                    (
                      context.partitionId,
                      CleanupIterator
                        .flatten(partition.map(_._2))
                        .iter { iter =>
                          val conditions = conditionsBc.value.zipWithIndex
                          val candidates = iter.take(sample * SamplingMaxReadPerPartitionFactor)
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
                        },
                      take)
                  },
                  partitions)
              var matches = Set.empty[Int]
              totalResults = (totalResults ++ results).sortBy(-_._2.size)
              val matchingPartitions = totalResults.flatMap { case (p, c, t) =>
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
      val requiredBc =
        if (takeMap.isEmpty) sc.broadcast(takeMap)
        else {
          val firstIdx = takeMap.keySet.min
          sc.broadcast(
            takeMap.updated(
              firstIdx,
              takeMap(firstIdx) + (if (numRecords < sample) sample - numRecords else 0)))
        }
      val r = action(rdd.mapPartitionsWithIndex { (idx, p) =>
        val take = requiredBc.value
        if (take.keySet.contains(idx)) {
          var remaining = take(idx)
          IteratorUtil.whileDefined {
            if (remaining > 0 && p.hasNext) Some {
              val (k, records) = p.next
              (
                k,
                records.chain(_.take(remaining).map { r =>
                  remaining -= 1
                  r
                }))
            }
            else None
          }
        } else Iterator.empty
      })
      conditionsBc.destroy()
      requiredBc.destroy()
      r
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
      Try(out.get.close())
      Try(HdfsIO.delete(path))
    }
  }

  def splitMergeInputStreams(
      in: InputStream,
      positions: Iterator[(Long, Long)],
      buffered: Boolean = true): InputStream = {
    val split = IOUtil.splitStream(in, positions)
    val merged = new ChainedInputStream(split, nextOnError = true)
    if (buffered) {
      val buffer = IOUtil.copyToBuffer(merged)
      in.close()
      new CleanupInputStream(buffer.get.get, () => buffer.clear(false))
    } else {
      merged
    }
  }

  def splitUserPwUrl(
      url: String,
      defaultUser: Option[String] = None,
      defaultPw: Option[String] = None): (String, Option[(String, String)]) = {
    val (urlWithoutUser, userPwStr) = {
      val atIdx = url.indexOf("@")
      if (atIdx > 0) {
        val afterAt = url.drop(atIdx + 1)
        val beforeAt = url.take(atIdx)
        val lastSlashIdx = beforeAt.lastIndexOf('/')
        if (lastSlashIdx < 0) {
          (afterAt, Some(beforeAt))
        } else {
          (url.take(lastSlashIdx + 1) + afterAt, Some(beforeAt.drop(lastSlashIdx + 1)))
        }
      } else (url, None)
    }
    (
      urlWithoutUser,
      userPwStr
        .flatMap { userPw =>
          val colonIdx = userPw.indexOf(":")
          if (colonIdx < 0) {
            defaultPw.map((userPw, _))
          } else {
            Some((userPw.take(colonIdx), userPw.drop(colonIdx + 1)))
          }
        }
        .orElse {
          for {
            user <- defaultUser
            pw <- defaultPw
          } yield (user, pw)
        })
  }

  def userPwFromUrl(
      url: String,
      defaultUser: Option[String] = None,
      defaultPw: Option[String] = None): Option[(String, String)] = {
    splitUserPwUrl(url, defaultUser, defaultPw)._2
  }

  def insertUrlUser(url: String, username: String): String = {
    url.replace("://", s"://$username@")
  }
}

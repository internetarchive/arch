package org.archive.webservices.ars.io

import java.io.{BufferedOutputStream, File, FileOutputStream, OutputStream}
import java.nio.file.Files

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.{RddUtil, StringUtil}
import org.archive.webservices.ars.model.ArsCloudConf

import scala.reflect.ClassTag
import scala.util.Try

object IOHelper {
  def tempDir[R](action: String => R): R = {
    val tmpPath = new File(ArsCloudConf.localTempPath)
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

  def load[A: ClassTag, F: ClassTag](
      path: String,
      rdd: String => RDD[A],
      filter: RDD[A] => RDD[F],
      sample: Int = -1): RDD[F] = {
    if (sample < 0) filter(rdd(path))
    else
      HdfsIO
        .files(path)
        .toSeq
        .sorted
        .toIterator
        .map(rdd)
        .map(filter)
        .find(_.mapPartitions(p => Iterator(p.hasNext)).collect.head)
        .getOrElse(RddUtil.emptyRDD[F])
        .mapPartitions(_.take(sample))
  }

  def load[A: ClassTag](path: String, rdd: String => RDD[A], sample: Int): RDD[A] =
    load[A, A](path, rdd, identity, sample)

  def size(path: String): Long = HdfsIO.files(path).map(HdfsIO.length).sum

  def sizeStr(path: String): String = {
    val units = Seq("B", "KB", "MB", "GB", "TB", "PB")
    var bytes = size(path).toDouble
    var unitIdx = 0
    while (bytes > 1024 && unitIdx < units.length - 1) {
      unitIdx += 1
      bytes = bytes / 1024
    }
    StringUtil.formatNumber(bytes, 1) + " " + units(unitIdx)
  }
}

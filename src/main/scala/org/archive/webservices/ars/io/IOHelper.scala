package org.archive.webservices.ars.io

import java.io.{BufferedOutputStream, File, FileNotFoundException, FileOutputStream, OutputStream}
import java.nio.file.Files
import java.util

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.hadoop.fs.permission.FsPermission
import org.apache.hadoop.fs.{CreateFlag, Path}
import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.RddUtil
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.util.FormatUtil

import scala.reflect.ClassTag
import scala.util.Try

object IOHelper {
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

  def sample[F: ClassTag](rdd: RDD[F], sample: Int = -1): RDD[F] = {
    val data = rdd
    if (sample < 0) data
    else {
      data
        .mapPartitionsWithIndex((idx, p) => if (p.hasNext) Iterator(idx) else Iterator.empty)
        .take(1)
        .headOption match {
        case Some(sampleParitionIdx) =>
          data.mapPartitionsWithIndex((idx, p) =>
            if (idx == sampleParitionIdx) p.take(sample) else Iterator.empty)
        case None =>
          RddUtil.emptyRDD
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

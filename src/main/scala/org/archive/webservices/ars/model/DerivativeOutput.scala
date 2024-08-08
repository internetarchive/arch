package org.archive.webservices.ars.model

import _root_.io.circe._
import _root_.io.circe.syntax._
import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.processing.DerivationJobInstance
import org.archive.webservices.ars.util.FormatUtil
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.{DigestUtil, StringUtil}

import java.io.{BufferedInputStream, FileInputStream, InputStream}
import java.time.Instant
import scala.util.Try

trait DerivativeOutput {
  def filename: String
  def dir: String
  def fileType: String
  def mimeType: String
  def downloadName: String
  def size: Long
  def time: Long
  def lineCount: Long
  def checksums: Map[String, String]
  def prefixDownload(prefix: String): DerivativeOutput

  lazy val path: String = dir + "/" + filename

  lazy val sizeStr: String = IOHelper.sizeStr(path)

  lazy val timeStr: String = FormatUtil.instantTimeString(Instant.ofEpochMilli(time))

  lazy val lineCountStr: Option[String] =
    if (lineCount < 0) None else Some(StringUtil.formatNumber(lineCount, 0))

  lazy val accessToken: String = DigestUtil.sha1Base32(filename + size + time)

  def prefixDownload(instance: DerivationJobInstance): DerivativeOutput = {
    val timestamp = instance.info.finished.map(IOHelper.pathTimestamp).map(_ + "_")
    prefixDownload(instance.conf.inputSpec.id + "_" + timestamp.getOrElse(""))
  }
}

case class DerivativeOutputFile(
    filename: String,
    dir: String,
    fileType: String,
    mimeType: String,
    downloadName: String)
    extends DerivativeOutput {
  import DerivativeOutput._

  lazy val (size, time) = Try {
    val status = HdfsIO.fs.getFileStatus(new Path(path))
    (status.getLen, status.getModificationTime)
  }.getOrElse((0L, 0L))

  lazy val lineCount: Long = {
    val p = path + LineCountFileSuffix
    if (HdfsIO.exists(p)) Try(HdfsIO.lines(p).head.toLong).getOrElse(-1)
    else -1
  }

  lazy val checksums: Map[String, String] = {
    val p = path + ChecksumsFileSuffix
    if (HdfsIO.exists(p))
      parser
        .decode[Map[String, String]](HdfsIO.lines(p).mkString)
        .right
        .toOption
        .getOrElse(Map.empty)
    else Map.empty
  }

  def prefixDownload(prefix: String): DerivativeOutput =
    copy(downloadName = IOHelper.escapePath(prefix) + filename)
}

object DerivativeOutput {
  val LineCountFileSuffix = "_linecount"
  val ChecksumsFileSuffix = ".checksums"

  def apply(
      filename: String,
      dir: String,
      fileType: String,
      mimeType: String): DerivativeOutputFile = {
    DerivativeOutputFile(filename, dir, fileType, mimeType, filename)
  }

  def hashFile(in: InputStream): Map[String, String] = Map("md5" -> DigestUtil.md5Hex(in))

  def hashFile(in: InputStream, hdfsPath: String): Unit =
    HdfsIO.writeLines(
      hdfsPath + ChecksumsFileSuffix,
      Seq(hashFile(in).asJson.spaces4),
      overwrite = true)

  def hashFileLocal(localPath: String, hdfsPath: String): Unit = {
    val in = new BufferedInputStream(new FileInputStream(localPath))
    try {
      hashFile(in, hdfsPath)
    } finally {
      in.close()
    }
  }

  def hashFileHdfs(hdfsPath: String): Unit =
    HdfsIO.access(hdfsPath, decompress = false)(hashFile(_, hdfsPath))
}

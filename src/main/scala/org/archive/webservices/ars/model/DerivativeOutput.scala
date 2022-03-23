package org.archive.webservices.ars.model

import java.io.{BufferedInputStream, FileInputStream, InputStream}
import java.time.Instant

import org.apache.hadoop.fs.Path
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.{DigestUtil, StringUtil}
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.util.FormatUtil
import _root_.io.circe._
import _root_.io.circe.syntax._

import scala.util.Try

case class DerivativeOutput(filename: String, dir: String, fileType: String, mimeType: String) {
  import DerivativeOutput._

  lazy val path: String = dir + "/" + filename

  lazy val (size, time) = {
    val status = HdfsIO.fs.getFileStatus(new Path(path))
    (status.getLen, status.getModificationTime)
  }
  lazy val sizeStr: String = IOHelper.sizeStr(path)

  lazy val lineCount: Long = {
    val p = path + LineCountFileSuffix
    if (HdfsIO.exists(p)) Try(HdfsIO.lines(p).head.toLong).getOrElse(-1)
    else -1
  }

  lazy val lineCountStr: Option[String] =
    if (lineCount < 0) None else Some(StringUtil.formatNumber(lineCount, 0))

  lazy val checksums: Map[String, String] = {
    val p = path + ChecksumsFileSuffix
    if (HdfsIO.exists(p))
      parser.decode[Map[String, String]](HdfsIO.lines(p).mkString).right.toOption.getOrElse(Map.empty)
    else Map.empty
  }

  lazy val timeStr: String = FormatUtil.instantTimeString(Instant.ofEpochMilli(time))

  lazy val accessToken: String = DigestUtil.sha1Base32(filename + size + time)
}

object DerivativeOutput {
  val LineCountFileSuffix = "_linecount"
  val ChecksumsFileSuffix = ".checksums"

  def hashFile(in: InputStream): Map[String, String] = Map("md5" -> DigestUtil.md5Hex(in))

  def hashFile(in: InputStream, hdfsPath: String): Unit = HdfsIO.writeLines(hdfsPath + ChecksumsFileSuffix, Seq(hashFile(in).asJson.spaces4), overwrite = true)

  def hashFileLocal(localPath: String, hdfsPath: String): Unit = {
    val in = new BufferedInputStream(new FileInputStream(localPath))
    try {
      hashFile(in, hdfsPath)
    } finally {
      in.close()
    }
  }

  def hashFileHdfs(hdfsPath: String): Unit = HdfsIO.access(hdfsPath, decompress = false)(hashFile(_, hdfsPath))
}

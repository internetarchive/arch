package org.archive.webservices.ars.model

import java.time.Instant

import org.apache.hadoop.fs.Path
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.StringUtil
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.util.FormatUtil

import scala.util.Try

case class DerivativeOutput(filename: String, dir: String, mimeType: String) {
  import DerivativeOutput._

  lazy val path: String = dir + "/" + filename

  lazy val (size, time) = {
    val status = HdfsIO.fs.getFileStatus(new Path(path))
    (status.getLen, status.getModificationTime)
  }
  lazy val sizeStr: String = IOHelper.sizeStr(path)

  lazy val lineCount: Long =
    if (HdfsIO.exists(path + lineCountFileSuffix))
      Try(HdfsIO.lines(path + lineCountFileSuffix).head.toLong).getOrElse(-1)
    else -1

  lazy val lineCountStr: Option[String] =
    if (lineCount < 0) None else Some(StringUtil.formatNumber(lineCount, 0))

  lazy val timeStr: String = FormatUtil.instantTimeString(Instant.ofEpochMilli(time))
}

object DerivativeOutput {
  val lineCountFileSuffix = "_linecount"
}

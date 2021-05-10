package org.archive.webservices.ars.model

import java.time.Instant

import org.apache.hadoop.fs.Path
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.webservices.ars.io.IOHelper

case class DerivativeOutput(filename: String, dir: String, mimeType: String) {
  lazy val path: String = dir + "/" + filename
  lazy val (size, time) = {
    val status = HdfsIO.fs.getFileStatus(new Path(path))
    (status.getLen, status.getModificationTime)
  }
  lazy val sizeStr: String = IOHelper.sizeStr(path)
  lazy val timeStr: String =
    Instant.ofEpochMilli(time).toString.stripSuffix("Z").replace("T", " ")
}

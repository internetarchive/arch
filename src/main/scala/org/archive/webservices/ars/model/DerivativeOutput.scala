package org.archive.webservices.ars.model

import org.archive.helge.sparkling.io.HdfsIO
import org.archive.webservices.ars.io.IOHelper

case class DerivativeOutput(filename: String, dir: String, mimeType: String) {
  lazy val path: String = dir + "/" + filename
  lazy val size: Long = HdfsIO.length(path)
  lazy val sizeStr: String = IOHelper.sizeStr(path)
}

package org.archive.webservices.ars.io

import org.archive.webservices.ars.io.FilePointer.SourceSeparator
import org.archive.webservices.sparkling.util.StringUtil

case class FilePointer(url: String, filename: String) {
  private lazy val sourcePathSplit = {
    val splitAt = StringUtil.prefixBySeparator(url, "/").lastIndexOf(SourceSeparator)
    if (splitAt < 0) ("", url) else (url.take(splitAt), url.drop(splitAt + 1))
  }

  def source: String = sourcePathSplit._1
  def path: String = sourcePathSplit._2
}

object FilePointer {
  val SourceSeparator = ":"

  def fromUrl(url: String): FilePointer = {
    val lastSlashIdx = url.lastIndexOf('/')
    if (lastSlashIdx < 0) {
      val sourceSeparatorIdx = url.lastIndexOf(SourceSeparator)
      if (sourceSeparatorIdx < 0) FilePointer(url, url)
      else FilePointer(url, url.drop(sourceSeparatorIdx + 1))
    } else FilePointer(url, url.drop(lastSlashIdx + 1))
  }
}

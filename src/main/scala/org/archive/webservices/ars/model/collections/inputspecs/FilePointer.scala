package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.collections.inputspecs.FilePointer.SourceSeparator
import org.archive.webservices.sparkling.util.StringUtil

case class FilePointer(url: String, filename: String) {
  lazy val source: String = StringUtil.prefixBySeparator(url, SourceSeparator)
  lazy val path: String = StringUtil.stripPrefixBySeparator(url, SourceSeparator)

  def isHttpSource = source.toLowerCase == "http" || source.toLowerCase == "https"
  def isCollectionSource = !isHttpSource && ArchCollection.prefix(source).isDefined
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

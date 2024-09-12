package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.archive.webservices.sparkling.io.IOUtil

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}

trait LocalFileCache {
  @transient private var _localCacheFile: Option[File] = None

  def isLocalCached: Boolean = _localCacheFile.isDefined

  def localCacheFile: Option[File] = _localCacheFile

  def cacheLocal(): File = _localCacheFile.getOrElse {
    synchronized(_localCacheFile.getOrElse {
      val file = IOUtil.tmpFile
      val out = IOUtil.fileOut(file)
      try {
        IOUtil.copy(payloadAccess, out)
      } finally out.close()
      _localCacheFile = Some(file)
      file
    })
  }

  def localFileCache: Option[InputStream] = _localCacheFile.map { file =>
    new BufferedInputStream(new FileInputStream(file))
  }

  def payloadAccess: InputStream
}

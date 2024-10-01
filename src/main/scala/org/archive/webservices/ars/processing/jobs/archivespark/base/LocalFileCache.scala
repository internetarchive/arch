package org.archive.webservices.ars.processing.jobs.archivespark.base

import io.circe.Json
import org.archive.webservices.archivespark.model.EnrichRoot
import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.logging.{Log, LogContext}

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}

trait LocalFileCache { this: EnrichRoot =>
  implicit private val logContext: LogContext = LogContext(this)

  @transient private var _localCacheFile: Option[File] = None

  def isLocalCached: Boolean = _localCacheFile.isDefined

  def localCacheFile: Option[File] = _localCacheFile

  def cacheLocal(): File = _localCacheFile.getOrElse {
    synchronized(_localCacheFile.getOrElse {
      val file = IOUtil.tmpFile
      Log.info(s"Caching to ${file.getPath}...")
      val out = IOUtil.fileOut(file)
      try {
        IOUtil.copy(payloadAccess, out)
      } finally out.close()
      _localCacheFile = Some(file)
      file
    })
  }

  override def toJson: Map[String, Json] = {
    synchronized {
      for (file <- _localCacheFile) file.delete()
      _localCacheFile = None
    }
    this.toJson
  }

  def localFileCache: Option[InputStream] = _localCacheFile.map { file =>
    new BufferedInputStream(new FileInputStream(file))
  }

  def payloadAccess: InputStream
}

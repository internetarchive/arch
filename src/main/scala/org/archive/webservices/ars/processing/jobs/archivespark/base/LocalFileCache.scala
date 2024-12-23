package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.commons.io.input.BoundedInputStream
import org.archive.webservices.archivespark.util.Bytes
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.logging.{Log, LogContext}

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import scala.util.Try

object LocalFileCache {
  val MaxMemoryCacheSize: Long = 1.mb
}

trait LocalFileCache {
  implicit private val logContext: LogContext = LogContext(this)

  @transient private var _memoryCache: Option[Array[Byte]] = None
  @transient private var _localCacheFile: Option[File] = None

  @transient var cacheEnabled = false

  def isLocalCached: Boolean = _localCacheFile.isDefined

  def localCacheFile: Option[File] = _localCacheFile

  private def cacheLocal(in: => Option[InputStream]): File = _localCacheFile.getOrElse {
    synchronized(_localCacheFile.getOrElse {
      val file = IOUtil.tmpFile
      Log.info(s"Caching to ${file.getPath}...")
      val out = IOUtil.fileOut(file)
      try {
        for (bytes <- _memoryCache) {
          out.write(bytes)
          _memoryCache = None
        }
        for (s <- in) {
          try {
            IOUtil.copy(s, out)
          } finally {
            s.close()
          }
        }
      } finally out.close()
      _localCacheFile = Some(file)
      Log.info(s"Cached ${file.getPath}.")
      file
    })
  }

  def cacheLocal(): File = cacheLocal(if (_memoryCache.isDefined) None else Some(payloadAccess))

  def clearCache(): Unit = if (_localCacheFile.isDefined || _memoryCache.isDefined) synchronized {
    for (file <- _localCacheFile) file.delete()
    _localCacheFile = None
    _memoryCache = None
  }

  def localFileCache: Option[InputStream] = _localCacheFile.map { file =>
    new BufferedInputStream(new FileInputStream(file))
  }

  def cachePayload(): Unit = if (_memoryCache.isEmpty && _localCacheFile.isEmpty) {
    synchronized {
      if (_memoryCache.isEmpty && _localCacheFile.isEmpty) {
        val in = payloadAccess
        try {
          val bounded = new BoundedInputStream(in, LocalFileCache.MaxMemoryCacheSize + 1)
          val array = IOUtil.bytes(bounded)
          _memoryCache = Some(array)
          if (array.length > LocalFileCache.MaxMemoryCacheSize) cacheLocal(Some(in))
        } catch {
          case e: Exception =>
            // skip if payload can't be read, e.g. malformed HTTP stream / decoding error
            Log.error(e.getMessage)
        } finally {
          Try(in.close())
        }
      }
    }
  }

  def cachedPayload: Bytes = Bytes.either({
    if (cacheEnabled) cachePayload()
    _memoryCache.map(Left(_)).getOrElse {
      _localCacheFile.map(file => Right(new FileInputStream(file))).getOrElse {
        Right(payloadAccess)
      }
    }
  })

  def payloadAccess: InputStream
}

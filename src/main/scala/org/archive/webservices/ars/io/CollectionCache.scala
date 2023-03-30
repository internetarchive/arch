package org.archive.webservices.ars.io

import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.io.HdfsIO._

import java.time.Instant
import scala.util.Try

object CollectionCache {
  val CacheClearThresholdBytes: Long = 1.tb

  private var inUse = Set.empty[String]
  private var lastUse = Map.empty[String, Long]

  def cache[R](sourceId: String)(action: String => R): R = {
    val dir = cacheDir(sourceId)
    synchronized {
      inUse += dir
      clearCache()
    }
    val path = ArchConf.collectionCachePath + "/" + dir
    fs.mkdirs(new Path(path))
    val r = action(path)
    synchronized {
      inUse -= dir
      lastUse = lastUse.updated(dir, Instant.now.toEpochMilli)
    }
    r
  }

  def cacheDir(sourceId: String): String = IOHelper.escapePath(sourceId)

  def cacheDirPath(cacheDir: String): String = ArchConf.collectionCachePath + "/" + cacheDir

  def cachePath(sourceId: String): String = cacheDirPath(cacheDir(sourceId))

  def cachePath(sourceId: String, filename: String): String = cachePath(sourceId) + "/" + filename

  def clearCache(): Unit = synchronized {
    var length = Try(fs.getContentSummary(new Path(ArchConf.collectionCachePath)).getLength)
      .getOrElse(0L)
    if (length > CacheClearThresholdBytes) {
      for (dir <- fs.listStatus(new Path(ArchConf.collectionCachePath))
           if dir.isDirectory) {
        val path = dir.getPath
        val c = path.getName
        if (!inUse.contains(c) && !lastUse.contains(c)) {
          val pathLength = fs.getContentSummary(path).getLength
          if (fs.delete(path, true)) length -= pathLength
        }
      }
      val toDelete =
        lastUse.toSeq
          .filter { case (c, _) => !inUse.contains(c) }
          .sortBy(_._2)
          .map(_._1)
          .toIterator
      while (length > CacheClearThresholdBytes && toDelete.hasNext) {
        val path = new Path(ArchConf.collectionCachePath + "/" + toDelete.next)
        val pathLength = fs.getContentSummary(path).getLength
        if (fs.delete(path, true)) length -= pathLength
      }
    }
  }
}

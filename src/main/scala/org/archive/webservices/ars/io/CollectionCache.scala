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

  def cache[R](collectionId: String)(action: String => R): R = {
    synchronized {
      inUse += collectionId
      clearCache()
    }
    val path = cachePath(collectionId)
    fs.mkdirs(new Path(path))
    val r = action(path)
    synchronized {
      inUse -= collectionId
      lastUse = lastUse.updated(collectionId, Instant.now.toEpochMilli)
    }
    r
  }

  def cachePath(collectionId: String): String =
    ArchConf.collectionCachePath + "/" + collectionId

  def clearCache(): Unit = synchronized {
    var length = Try(fs.getContentSummary(new Path(ArchConf.collectionCachePath)).getLength)
      .getOrElse(0L)
    if (length > CacheClearThresholdBytes) {
      for (dir <- fs.listStatus(new Path(ArchConf.collectionCachePath))
           if dir.isDirectory) {
        val path = dir.getPath
        val collectionId = path.getName
        if (!inUse.contains(collectionId) && !lastUse.contains(collectionId)) {
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
        val path = new Path(cachePath(toDelete.next))
        val pathLength = fs.getContentSummary(path).getLength
        if (fs.delete(path, true)) length -= pathLength
      }
    }
  }
}

package org.archive.webservices.ars.model

import java.time.Instant

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Json
import org.archive.helge.sparkling.io.HdfsIO
import org.scalatra.guavaCache.GuavaCache

import scala.collection.immutable.ListMap

case class ArchCollectionInfo private (
    collectionId: String,
    lastJob: Option[(String, Long)] = None) {
  def lastJobName: Option[String] = lastJob.map(_._1)
  def lastJobTime: Option[Instant] = lastJob.map(_._2).map(Instant.ofEpochSecond)

  def setLastJob(id: String, time: Instant): ArchCollectionInfo = {
    copy(collectionId, Some(id, time.getEpochSecond))
  }

  def save(): Unit = {
    val file = ArchCollectionInfo.infoFile(collectionId)
    GuavaCache.put(ArchCollectionInfo.CachePrefix + file, this, None)
    HdfsIO.writeLines(
      file,
      Seq((ListMap.empty[String, Json] ++ {
        lastJob.toSeq.flatMap {
          case (name, time) =>
            Seq("lastJobName" -> name.asJson, "lastJobEpoch" -> time.asJson)
        }
      }).asJson.spaces4),
      overwrite = true)
  }
}

object ArchCollectionInfo {
  val Charset = "utf-8"
  val CachePrefix = "collection-info#"

  def infoFile(collectionId: String): String =
    ArchConf.jobOutPath + s"/$collectionId/info.json"

  def get(collectionId: String): ArchCollectionInfo = {
    val file = infoFile(collectionId)
    GuavaCache.get(CachePrefix + file).getOrElse {
      val info = if (HdfsIO.exists(file)) {
        parse(HdfsIO.lines(file).mkString).right.toOption.map(_.hcursor) match {
          case Some(cursor) =>
            val lastJob = for {
              name <- cursor.get[String]("lastJobName").toOption
              epoch <- cursor.get[Long]("lastJobEpoch").toOption
            } yield (name, epoch)
            ArchCollectionInfo(collectionId, lastJob)
          case None => ArchCollectionInfo(collectionId)
        }
      } else ArchCollectionInfo(collectionId)
      GuavaCache.put(CachePrefix + file, info, None)
    }
  }
}

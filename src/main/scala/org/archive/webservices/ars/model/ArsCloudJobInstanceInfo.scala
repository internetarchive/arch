package org.archive.webservices.ars.model

import java.time.Instant

import _root_.io.circe.syntax._
import io.circe.Json
import io.circe.parser.parse
import org.archive.helge.sparkling.io.HdfsIO
import org.scalatra.guavaCache.GuavaCache

import scala.collection.immutable.ListMap

case class ArsCloudJobInstanceInfo private (started: Long = -1, finished: Long = -1) {
  def startTime: Option[Instant] = Some(started).filter(_ != -1).map(Instant.ofEpochSecond)
  def finishedTime: Option[Instant] = Some(finished).filter(_ != -1).map(Instant.ofEpochSecond)

  def setStartTime(time: Instant): ArsCloudJobInstanceInfo = {
    copy(started = time.getEpochSecond)
  }

  def setFinishedTime(time: Instant): ArsCloudJobInstanceInfo = {
    copy(finished = time.getEpochSecond)
  }

  def save(jobOutPath: String): Unit = {
    val file = ArsCloudJobInstanceInfo.infoFile(jobOutPath)
    GuavaCache.put(ArsCloudJobInstanceInfo.CachePrefix + file, this, None)
    HdfsIO.writeLines(
      file,
      Seq((ListMap.empty[String, Json] ++ {
        Iterator(started).filter(_ != -1).map("started" -> _.asJson)
      } ++ {
        Iterator(finished).filter(_ != -1).map("finished" -> _.asJson)
      }).asJson.spaces4),
      overwrite = true)
  }
}

object ArsCloudJobInstanceInfo {
  val Charset = "utf-8"
  val CachePrefix = "job-instance-info#"

  def infoFile(jobOutPath: String): String =
    jobOutPath + "/info.json"

  def get(jobOutPath: String): ArsCloudJobInstanceInfo = {
    val file = infoFile(jobOutPath)
    GuavaCache.get(CachePrefix + file).getOrElse {
      val info = if (HdfsIO.exists(file)) {
        parse(HdfsIO.lines(file).mkString).right.toOption.map(_.hcursor) match {
          case Some(cursor) =>
            val started = cursor.get[Long]("started").toOption.getOrElse(-1L)
            val finished = cursor.get[Long]("finished").toOption.getOrElse(-1L)
            ArsCloudJobInstanceInfo(started, finished)
          case None => ArsCloudJobInstanceInfo()
        }
      } else ArsCloudJobInstanceInfo()
      GuavaCache.put(CachePrefix + file, info, None)
    }
  }
}

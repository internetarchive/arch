package org.archive.webservices.ars.model

import _root_.io.circe.syntax._
import io.circe.parser.parse
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.sparkling.io.HdfsIO
import org.scalatra.guavaCache.GuavaCache

import java.time.Instant
import scala.collection.immutable.ListMap

class ArchJobInstanceInfo private () {
  var uuid: Option[String] = None
  var conf: Option[DerivationJobConf] = None
  var started: Option[Instant] = None
  var finished: Option[Instant] = None

  def save(jobOutPath: String): Unit = {
    val file = ArchJobInstanceInfo.infoFile(jobOutPath)
    GuavaCache.put(ArchJobInstanceInfo.CachePrefix + file, this, None)
    HdfsIO.writeLines(
      file,
      Seq((ListMap(uuid.map("uuid" -> _.asJson).toSeq: _*) ++ {
        conf.map("conf" -> _.toJson)
      } ++ {
        started.map("started" -> _.getEpochSecond.asJson)
      } ++ {
        finished.map("finished" -> _.getEpochSecond.asJson)
      }).asJson.spaces4),
      overwrite = true)
  }
}

object ArchJobInstanceInfo {
  val Charset = "utf-8"
  val CachePrefix = "job-instance-info#"
  val InfoFile = "info.json"

  def infoFile(jobOutPath: String): String = jobOutPath + s"/$InfoFile"

  def apply(jobOutPath: String): ArchJobInstanceInfo = {
    val file = infoFile(jobOutPath)
    GuavaCache.get(CachePrefix + file).getOrElse {
      val info = if (HdfsIO.exists(file)) {
        parse(HdfsIO.lines(file).mkString).right.toOption.map(_.hcursor) match {
          case Some(cursor) =>
            val info = new ArchJobInstanceInfo()
            info.uuid = cursor.get[String]("uuid").toOption
            info.conf = cursor.downField("conf").focus.flatMap(DerivationJobConf.fromJson)
            info.started = cursor.get[Long]("started").toOption.map(Instant.ofEpochSecond)
            info.finished = cursor.get[Long]("finished").toOption.map(Instant.ofEpochSecond)
            info
          case None => new ArchJobInstanceInfo()
        }
      } else new ArchJobInstanceInfo()
      GuavaCache.put(CachePrefix + file, info, None)
    }
  }
}

package org.archive.webservices.ars.model

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Json
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.archive.webservices.sparkling.io.HdfsIO
import org.scalatra.guavaCache.GuavaCache

import java.time.Instant
import scala.collection.immutable.ListMap

case class ArchCollectionInfo private (
    collectionId: String,
    lastJob: Option[(String, Boolean, Long)] = None) {
  def lastJobId: Option[String] = lastJob.map(_._1)
  def lastJobSample: Option[Boolean] = lastJob.map(_._2)
  def lastJobTime: Option[Instant] = lastJob.map(_._3).map(Instant.ofEpochSecond)
  def lastJobName: Option[String] = lastJobId.flatMap(JobManager.jobs.get).map { job =>
    job.name + (if (lastJobSample.getOrElse(false)) ArchCollectionInfo.SampleNameSuffix else "")
  }

  def setLastJob(id: String, sample: Boolean, time: Instant): ArchCollectionInfo = {
    copy(collectionId, Some(id, sample, time.getEpochSecond))
  }

  def save(): Unit = {
    val file = ArchCollectionInfo.infoFile(collectionId)
    GuavaCache.put(ArchCollectionInfo.CachePrefix + file, this, None)
    HdfsIO.writeLines(
      file,
      Seq((ListMap.empty[String, Json] ++ {
        lastJob.toSeq.flatMap {
          case (id, sample, time) =>
            Seq(
              "lastJobId" -> id.asJson,
              "lastJobSample" -> sample.asJson,
              "lastJobEpoch" -> time.asJson)
        }
      }).asJson.spaces4),
      overwrite = true)
  }
}

object ArchCollectionInfo {
  val Charset = "utf-8"
  val CachePrefix = "collection-info#"
  val SampleNameSuffix = " (Sample)"

  def infoFile(collectionId: String): String = {
    DerivationJobConf.collectionOutPath(collectionId) + "/info.json"
  }

  def get(collectionId: String): ArchCollectionInfo = {
    val file = infoFile(collectionId)
    GuavaCache.get(CachePrefix + file).getOrElse {
      val info = if (HdfsIO.exists(file)) {
        parse(HdfsIO.lines(file).mkString).right.toOption.map(_.hcursor) match {
          case Some(cursor) =>
            val lastJob = cursor.get[Long]("lastJobEpoch").toOption.flatMap { epoch =>
              cursor
                .get[String]("lastJobId")
                .toOption
                .map { id =>
                  (id, cursor.get[Boolean]("lastJobSample").getOrElse(false), epoch)
                }
                .orElse {
                  cursor.get[String]("lastJobName").toOption.flatMap { name =>
                    JobManager.nameLookup.get(name.stripSuffix(SampleNameSuffix)).map { job =>
                      (job.id, name.endsWith(SampleNameSuffix), epoch)
                    }
                  }
                }
            }
            ArchCollectionInfo(collectionId, lastJob)
          case None => ArchCollectionInfo(collectionId)
        }
      } else ArchCollectionInfo(collectionId)
      GuavaCache.put(CachePrefix + file, info, None)
    }
  }
}

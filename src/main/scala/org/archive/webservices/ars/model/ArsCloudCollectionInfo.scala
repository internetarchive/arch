package org.archive.webservices.ars.model

import java.time.Instant

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Decoder
import org.archive.helge.sparkling.io.HdfsIO

case class ArsCloudCollectionInfo private (
    collectionId: String,
    lastJob: Option[(String, Long)]) {
  def lastJobName: Option[String] = lastJob.map(_._1)
  def lastJobTime: Option[Instant] = lastJob.map(_._2).map(Instant.ofEpochSecond)

  def setLastJob(id: String, time: Instant): ArsCloudCollectionInfo = {
    copy(collectionId, Some(id, time.getEpochSecond))
  }

  def save(): Unit = {
    val file = ArsCloudCollectionInfo.infoFile(collectionId)
    HdfsIO.writeLines(
      file,
      Seq(ArsCloudCollectionInfo.unapply(this).get.asJson.spaces4),
      overwrite = true)
  }
}

object ArsCloudCollectionInfo {
  val Charset = "utf-8"

  def infoFile(collectionId: String): String =
    ArsCloudConf.jobOutPath + s"/$collectionId/info.json"

  def get(collectionId: String): ArsCloudCollectionInfo = {
    val file = infoFile(collectionId)
    val str = if (HdfsIO.exists(file)) {
      HdfsIO.lines(file).mkString
    } else ""
    def opt[A](apply: A => ArsCloudCollectionInfo)(
        implicit decoder: Decoder[A]): Option[ArsCloudCollectionInfo] =
      parse(str).right.toOption.flatMap(_.as[A].toOption).map(apply)
    opt((ArsCloudCollectionInfo.apply _).tupled)
      .map(_.copy(collectionId = collectionId))
      .getOrElse {
        ArsCloudCollectionInfo(collectionId, None)
      }
  }
}

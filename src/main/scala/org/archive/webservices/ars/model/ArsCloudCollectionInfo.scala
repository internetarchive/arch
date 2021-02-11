package org.archive.webservices.ars.model

import java.io.{File, PrintStream}
import java.time.Instant

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Decoder

import scala.io.Source

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
    val out = new PrintStream(file, ArsCloudCollectionInfo.Charset)
    try {
      out.println(ArsCloudCollectionInfo.unapply(this).get.asJson.spaces4)
    } finally {
      out.close()
    }
  }
}

object ArsCloudCollectionInfo {
  val Charset = "utf-8"

  def infoFile(collectionId: String): File =
    new File(ArsCloudConf.jobOutPath + s"/$collectionId/info.json")

  def get(collectionId: String): ArsCloudCollectionInfo = {
    val file = infoFile(collectionId)
    val str = if (file.exists) {
      val source = Source.fromFile(file, Charset)
      try {
        source.mkString
      } catch {
        case _: Exception => ""
      } finally {
        source.close()
      }
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

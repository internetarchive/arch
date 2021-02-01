package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.ArsCloudConf
import _root_.io.circe.syntax._
import _root_.io.circe.parser._
import io.circe.{Decoder, Json}

case class DerivationJobConf(collectionId: String, inputPath: String, outputPath: String) {
  def serialize: String = DerivationJobConf.unapply(this).get.asJson.noSpaces
}

object DerivationJobConf {
  def collection(collectionId: String): Option[DerivationJobConf] = {
    if (collectionId.startsWith("ARCHIVEIT-")) Some(aitCollection(collectionId))
    else None
  }

  def aitCollection(collectionId: String): DerivationJobConf = {
    val aitId = collectionId.stripPrefix("ARCHIVEIT-")
    val inputPath = ArsCloudConf.aitCollectionPath + s"/$aitId/" + ArsCloudConf.aitCollectionWarcDir + "/*.warc.gz"
    val outputPath = ArsCloudConf.jobOutPath + s"/$collectionId/out"
    DerivationJobConf(collectionId, inputPath, outputPath)
  }

  def deserialize(conf: String): DerivationJobConf = {
    def getOrElse[A](json: Json, orElse: A)(implicit decoder: Decoder[A]): A =
      json.as[A].getOrElse(orElse)
    (DerivationJobConf.apply _).tupled(getOrElse(parse(conf).toOption.get, ("", "", "")))
  }
}

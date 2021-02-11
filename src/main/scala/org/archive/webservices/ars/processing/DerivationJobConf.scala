package org.archive.webservices.ars.processing

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.{Decoder, Json}
import org.archive.webservices.ars.model.{ArsCloudCollection, ArsCloudConf}

case class DerivationJobConf(
    collectionId: String,
    inputPath: String,
    outputPath: String,
    infoFilePath: String,
    sample: Int = -1) {
  def serialize: String = DerivationJobConf.unapply(this).get.asJson.noSpaces
}

object DerivationJobConf {
  val SampleSize = 100

  def collection(collectionId: String, sample: Boolean = false): Option[DerivationJobConf] = {
    if (collectionId.startsWith(ArsCloudCollection.AitPrefix))
      Some(aitCollection(collectionId, sample))
    else None
  }

  def aitCollection(collectionId: String, sample: Boolean = false): DerivationJobConf = {
    val aitId = collectionId.stripPrefix(ArsCloudCollection.AitPrefix)
    val inputPath = ArsCloudConf.aitCollectionPath + s"/$aitId/" + ArsCloudConf.aitCollectionWarcDir + "/*.warc.gz"
    val outDir = if (sample) "samples" else "out"
    val outputPath = ArsCloudConf.jobOutPath + s"/$collectionId/" + outDir
    val infoFilePath = ArsCloudConf.jobOutPath + s"/$collectionId/info.json"
    DerivationJobConf(
      collectionId,
      inputPath,
      outputPath,
      infoFilePath,
      if (sample) SampleSize else -1)
  }

  def deserialize(conf: String): DerivationJobConf = {
    def getOrElse[A](json: Json, orElse: A)(implicit decoder: Decoder[A]): A =
      json.as[A].getOrElse(orElse)
    (DerivationJobConf.apply _).tupled(getOrElse(parse(conf).toOption.get, ("", "", "", "", -1)))
  }
}

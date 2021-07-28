package org.archive.webservices.ars.processing

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Decoder
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}

case class DerivationJobConf(
    collectionId: String,
    inputPath: String,
    outputPath: String,
    sample: Int = -1) {
  def isSample: Boolean = sample >= 0
  def serialize: String = DerivationJobConf.unapply(this).get.asJson.noSpaces
}

object DerivationJobConf {
  val SampleSize = 100

  def collection(collectionId: String, sample: Boolean = false): Option[DerivationJobConf] = {
    ArchCollection.inputPath(collectionId).map { inputPath =>
      val outDir = if (sample) "samples" else "out"
      val outputPath = ArchConf.jobOutPath + s"/$collectionId/" + outDir
      DerivationJobConf(collectionId, inputPath, outputPath, if (sample) SampleSize else -1)
    }
  }

  def deserialize(conf: String): Option[DerivationJobConf] = {
    def opt[A](apply: A => DerivationJobConf)(
        implicit decoder: Decoder[A]): Option[DerivationJobConf] =
      parse(conf).right.toOption.flatMap(_.as[A].toOption).map(apply)
    opt((DerivationJobConf.apply _).tupled)
  }
}

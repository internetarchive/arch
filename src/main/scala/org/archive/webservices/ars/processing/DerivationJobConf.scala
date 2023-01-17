package org.archive.webservices.ars.processing

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Json
import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.{CollectionSpecifics, CustomCollectionSpecifics}

import java.time.Instant

case class DerivationJobConf(
    collectionId: String,
    inputPath: String,
    outputPath: String,
    sample: Int = -1,
    params: DerivationJobParameters = DerivationJobParameters.Empty) {
  def isSample: Boolean = sample >= 0
  def serialize: String = toJson.noSpaces
  def toJson: Json = (collectionId, inputPath, outputPath, sample, params.toJson).asJson
}

object DerivationJobConf {
  val SampleSize = 100

  def collectionOutPath(collectionId: String): String = {
    ArchConf.jobOutPath + "/" + IOHelper.escapePath(collectionId)
  }

  def collection(collectionId: String, sample: Boolean = false)(implicit context: RequestContext = RequestContext.None): Option[DerivationJobConf] = {
    CollectionSpecifics.get(collectionId, context.user).map { collection =>
      val outDir = if (sample) "samples" else "out"
      val outputPath = collectionOutPath(collection.id) + "/" + outDir
      DerivationJobConf(collectionId, collection.inputPath, outputPath, if (sample) SampleSize else -1)
    }
  }

  def userDefinedQuery(collectionId: String, params: DerivationJobParameters)(implicit context: RequestContext = RequestContext.None): Option[DerivationJobConf] = {
    context.userOpt.flatMap { user =>
      CollectionSpecifics.get(collectionId, context.user).map { collection =>
        val userPath = CustomCollectionSpecifics.path(user)
        val outPath = new Path(userPath, IOHelper.escapePath(collection.id) + "_" + Instant.now.toEpochMilli).toString
        DerivationJobConf(collectionId, collection.inputPath, outPath, -1, params = params.set("location", collectionId))
      }
    }
  }

  def fromJson(json: Json): Option[DerivationJobConf] = {
    json.asArray.map(_.toIterator.buffered).flatMap { values =>
      for {
        collectionId <- values.next.asString
        inputPath <- values.next.asString
        outputPath <- values.next.asString
        sample <- values.next.asNumber.flatMap(_.toInt)
      } yield {
        val params = values.headOption.flatMap(DerivationJobParameters.fromJson).getOrElse(DerivationJobParameters.Empty)
        DerivationJobConf(collectionId, inputPath, outputPath, sample, params)
      }
    }
  }

  def deserialize(conf: String): Option[DerivationJobConf] =
    parse(conf).right.toOption.flatMap(fromJson)
}

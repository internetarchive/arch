package org.archive.webservices.ars.processing

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Json
import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.CustomCollectionSpecifics
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}

import java.time.Instant

case class DerivationJobConf(
    collectionId: String,
    inputPath: String,
    outputPath: String,
    sample: Int = -1,
    params: DerivationJobParameters = DerivationJobParameters.Empty) {
  def isSample: Boolean = sample >= 0
  def serialize: String = toJson.noSpaces
  def toJson: Json = {
    if (params.isEmpty) (collectionId, inputPath, outputPath, sample).asJson
    else (collectionId, inputPath, outputPath, sample, params.toJson).asJson
  }
}

object DerivationJobConf {
  val SampleSize = 100

  def jobOutpath(collection: ArchCollection, global: Boolean = false): String = {
    collection.userSpecificId.filter(_ => !global).map { case (userId, sourceId) =>
      ArchConf.jobOutPath + "/" + IOHelper.escapePath(userId + "/" + sourceId)
    }.getOrElse(ArchConf.globalJobOutPath + "/" + IOHelper.escapePath(collection.sourceId))
  }

  def collection(collection: ArchCollection, sample: Boolean = false, global: Boolean = false): Option[DerivationJobConf] = {
    collection.specifics.map { specifics =>
      val outDir = if (sample) "/samples" else "/out"
      DerivationJobConf(
        collection.id,
        specifics.inputPath,
        jobOutpath(collection, global) + outDir,
        if (sample) SampleSize else -1)
    }
  }

  def collection123(collectionId: String, sample: Boolean = false, global: Boolean = false)(
    implicit context: RequestContext = RequestContext.None): Option[DerivationJobConf] = {
    for {
      collection <- ArchCollection.get(collectionId)
      specifics <- collection.specifics
    } yield {
      val outDir = if (sample) "/samples" else "/out"
      DerivationJobConf(
        collection.id,
        specifics.inputPath,
        jobOutpath(collection, global) + outDir,
        if (sample) SampleSize else -1)
    }
  }

  def userDefinedQuery(collectionId: String, params: DerivationJobParameters)(
      implicit context: RequestContext = RequestContext.None): Option[DerivationJobConf] = {
    context.userOpt.flatMap { user =>
      for {
        collection <- ArchCollection.get(collectionId)
        specifics <- collection.specifics
      } yield {
        val userPath = CustomCollectionSpecifics.path(user)
        val outPath = new Path(
          userPath,
          IOHelper.escapePath(collection.id) + "_" + Instant.now.toEpochMilli).toString
        DerivationJobConf(
          collectionId,
          specifics.inputPath,
          outPath,
          -1,
          params = params.set("location", collectionId))
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
        val params = values.headOption
          .flatMap(DerivationJobParameters.fromJson)
          .getOrElse(DerivationJobParameters.Empty)
        DerivationJobConf(collectionId, inputPath, outputPath, sample, params)
      }
    }
  }

  def deserialize(conf: String): Option[DerivationJobConf] =
    parse(conf).right.toOption.flatMap(fromJson)
}

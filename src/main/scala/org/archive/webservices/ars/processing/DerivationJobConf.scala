package org.archive.webservices.ars.processing

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Json
import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.{CollectionSpecifics, CustomCollectionSpecifics, UnionCollectionSpecifics}
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery

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

  def jobOutPath(collection: ArchCollection, global: Boolean = false): String = {
    collection.userSpecificId
      .filter(_ => !global)
      .map {
        case (userId, sourceId) =>
          ArchConf.jobOutPath + "/" + IOHelper.escapePath(userId + "/" + sourceId)
      }
      .getOrElse(ArchConf.globalJobOutPath + "/" + IOHelper.escapePath(collection.sourceId))
  }

  def jobInPath(
      specifics: CollectionSpecifics,
      params: DerivationJobParameters = DerivationJobParameters.Empty)(
      implicit context: RequestContext): String = {
    if (specifics.id.startsWith(UnionCollectionSpecifics.Prefix)) {
      params
        .get[Array[String]]("input")
        .toSet
        .flatten
        .filter { collectionId =>
          ArchCollection.get(collectionId).isDefined
        }
        .mkString(",")
    } else specifics.inputPath
  }

  def collection(
      collection: ArchCollection,
      sample: Boolean = false,
      global: Boolean = false): Option[DerivationJobConf] = {
    collection.specifics.map { specifics =>
      val outDir = if (sample) "/samples" else "/out"
      DerivationJobConf(
        collection.id,
        specifics.inputPath,
        jobOutPath(collection, global) + outDir,
        if (sample) SampleSize else -1)
    }
  }

  def userDefinedQuery(collection: ArchCollection, params: DerivationJobParameters, sample: Boolean = false)(
      implicit context: RequestContext): Option[DerivationJobConf] = {
    context.userOpt.flatMap { user =>
      collection.specifics.map { specifics =>
        val collectionUserId = collection.userSpecificId
          .filter(_._1 == user.id)
          .map(_._2)
          .getOrElse(collection.sourceId)
        val outPath = new Path(
          CustomCollectionSpecifics.path(user),
          IOHelper.escapePath(collectionUserId + "_" + Instant.now.toEpochMilli)).toString
        DerivationJobConf(
          collection.id,
          jobInPath(specifics, params),
          outPath,
          if (sample) SampleSize else -1,
          params = params.set("location", collection.sourceId))
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

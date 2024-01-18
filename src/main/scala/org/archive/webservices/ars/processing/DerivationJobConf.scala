package org.archive.webservices.ars.processing

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.{HCursor, Json}
import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.inputspecs.{CollectionBasedInputSpec, InputSpec}
import org.archive.webservices.ars.model.collections.{CollectionSpecifics, CustomCollectionSpecifics, UnionCollectionSpecifics}
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}

import java.time.Instant
import scala.collection.immutable.ListMap

class DerivationJobConf private (
    val inputSpec: InputSpec,
    val outputPath: String,
    val sample: Int = -1,
    val params: DerivationJobParameters = DerivationJobParameters.Empty) extends Serializable {
  def isSample: Boolean = sample >= 0
  def toJson: Json = ListMap (
    "inputSpec" -> inputSpec.cursor.focus.get,
    "outputPath" -> outputPath.asJson,
    "sample" -> sample.asJson,
    "params" -> params.toJson
  ).asJson
  def serialize: String = toJson.noSpaces
}

object DerivationJobConf {
  val SampleSize = 100

  case class Identifier private (str: String)

  implicit def toIdentifier(conf: DerivationJobConf): Identifier = Identifier(conf.serialize)

  def jobOutPath(collection: ArchCollection, global: Boolean = false): String = {
    collection.userSpecificId
      .filter(_ => !global)
      .map { case (userId, sourceId) =>
        ArchConf.jobOutPath + "/" + IOHelper.escapePath(userId + "/" + sourceId)
      }
      .getOrElse(ArchConf.globalJobOutPath + "/" + IOHelper.escapePath(collection.sourceId))
  }

  def jobInPath(
      specifics: CollectionSpecifics,
      params: DerivationJobParameters = DerivationJobParameters.Empty)(implicit
      context: RequestContext): String = {
    if (specifics.id.startsWith(UnionCollectionSpecifics.Prefix)) {
      UnionCollectionSpecifics.collections(params).map(_.id).mkString(",")
    } else specifics.inputPath
  }

  def collectionInstance(
      jobId: String,
      conf: DerivationJobConf): Option[DerivationJobInstance] = {
    JobManager.getInstanceOrGlobal(
      jobId,
      conf,
      DerivationJobConf.collection(conf.inputSpec.collection, sample = conf.isSample, global = true))
  }

  def collectionInstance(
      jobId: String,
      collection: ArchCollection,
      sample: Boolean): Option[DerivationJobInstance] = {
    collectionInstance(
      jobId,
      DerivationJobConf.collection(collection, sample = sample, global = true))
  }

  def collection(
      collection: ArchCollection,
      params: DerivationJobParameters = DerivationJobParameters.Empty,
      sample: Boolean = false,
      global: Boolean = false): DerivationJobConf = {
    new DerivationJobConf(
      InputSpec.apply(collection, collection.specifics.inputPath),
      jobOutPath(collection, global) + (if (sample) "/samples" else "/out"),
      if (sample) SampleSize else -1, params)
  }

  def userDefinedQuery(
      collection: ArchCollection,
      params: DerivationJobParameters,
      sample: Boolean = false)(implicit context: RequestContext): Option[DerivationJobConf] = {
    context.userOpt.map { user =>
      val collectionUserId = collection.userSpecificId
        .filter(_._1 == user.id)
        .map(_._2)
        .getOrElse(collection.sourceId)
      val outPath = new Path(
        CustomCollectionSpecifics.path(user),
        IOHelper.escapePath(collectionUserId + "_" + Instant.now.toEpochMilli)).toString
      new DerivationJobConf(
        InputSpec(collection, jobInPath(collection.specifics, params)),
        outPath,
        if (sample) SampleSize else -1,
        params = params.set(
          "location",
          if (collection.id.startsWith(CustomCollectionSpecifics.Prefix)) {
            CustomCollectionSpecifics.location(collection.id).getOrElse(collection.sourceId)
          } else collection.sourceId))
    }
  }

  def fromJson(cursor: HCursor, sample: Boolean = false, outPath: Option[String] = None): Option[DerivationJobConf] = {
    for {
      outputPath <- cursor.get[String]("outputPath").toOption.orElse(outPath)
      spec <- cursor.downField("inputSpec").success.map(InputSpec(_))
    } yield {
      val sampleSize = cursor.get[Int]("sample").getOrElse(if (sample) SampleSize else -1)
      val params = cursor.downField("params").focus
        .flatMap(DerivationJobParameters.fromJson)
        .getOrElse(DerivationJobParameters.Empty)
      new DerivationJobConf(spec, outputPath, sampleSize, params)
    }
  }

  def fromJson(json: Json): Option[DerivationJobConf] = if (json.isArray) {
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
        new DerivationJobConf(InputSpec(collectionId, inputPath), outputPath, sample, params)
      }
    }
  } else {
    fromJson(json.hcursor)
  }

  def deserialize(conf: String): Option[DerivationJobConf] = parse(conf).right.toOption.flatMap(fromJson)
}

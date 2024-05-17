package org.archive.webservices.ars.processing.jobs.archivespark

import edu.stanford.nlp.pipeline.StanfordCoreNLP
import io.circe.{HCursor, Json}
import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.functions.{Entities, EntitiesConstants}
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.archivespark.model.dataloads.ByteLoad
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobParameters}
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkArchJob, LocalFileCache}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.{Whisper, WhisperText}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.{ArchArchiveSparkFunctionAdapter, EntitiesAdapter}

import java.io
import java.util.Properties
import scala.collection.JavaConverters.asScalaSetConverter

object ArchiveSparkFlexJob extends ArchiveSparkArchJob {
  val uuid: String = "018f52cc-d917-71ac-9e64-19fb219114a4"

  val name: String = id
  val description: String = "ArchiveSpark flex job "
  val category: ArchJobCategory = ArchJobCategories.None

  override def filter(rdd: RDD[ArchEnrichRoot[_]], conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = {
    val mime = conf.params.values.get("mime").toSeq.flatMap { mime =>
      if (mime.isString) Seq(mime.asString)
      else if (mime.isArray) mime.asArray.toSeq.flatMap(_.flatMap(_.asString))
      else Seq.empty
    }.toSet
    if (mime.isEmpty) rdd else rdd.mapPartitions { partition =>
      partition.filter(r => mime.contains(r.mime))
    }
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    conf.params.values.get("functions").toSeq.flatMap(_.asArray.toSeq.flatten).map { function =>
      ArchiveSparkFlexJob.initFunction(function)
    }
  }

  val adapters: Map[String, ArchArchiveSparkFunctionAdapter[_]] = Seq(
    EntitiesAdapter,
    Whisper,
    WhisperText
  ).flatMap { adapter =>
    Iterator(
      adapter.name -> adapter,
      adapter.name.toLowerCase -> adapter
    )
  }.toMap

  private def initFunction[A](func: ArchArchiveSparkFunctionAdapter[A], cursor: HCursor): EnrichFunc[ArchEnrichRoot[_], A, _] = {
    val dependency = cursor.downField("on").focus.map(initFunction).flatMap(func.toDependencyPointer)
    cursor.downField("params").focus.flatMap(DerivationJobParameters.fromJson) match {
      case Some(params) => func.withParams(params, on = dependency)
      case None => func.noParams(on = dependency)
    }
  }

  def initFunction(definition: Json): EnrichFunc[ArchEnrichRoot[_], _, _] = {
    if (definition.isString) {
      adapters.get(definition.asString.get).map(_.noParams)
    } else if (definition.isObject) {
      val cursor = definition.hcursor
      cursor.get[String]("name").toOption.flatMap { name =>
        adapters.get(name).map(initFunction(_, cursor))
      }
    } else None
  }.getOrElse {
    throw new UnsupportedOperationException()
  }
}
package org.archive.webservices.ars.processing.jobs.archivespark

import io.circe.{HCursor, Json}
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.{ArchArchiveSparkFunctionAdapter, EntitiesAdapter}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.{Whisper, WhisperText}
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobParameters}
import org.archive.webservices.sparkling.util.StringUtil

object ArchiveSparkFlexJob extends AiJob {
  val uuid: String = "018f52cc-d917-71ac-9e64-19fb219114a4"

  val name: String = id
  val description: String = "ArchiveSpark flex job "
  val category: ArchJobCategory = ArchJobCategories.None

  override def genericPredicate(conf: DerivationJobConf): ArchEnrichRoot[_] => Boolean = {
    val mime = conf.params.values
      .get("mime")
      .toSeq
      .flatMap { mime =>
        if (mime.isString) mime.asString.toSeq
        else if (mime.isArray) mime.asArray.toSeq.flatMap(_.flatMap(_.asString))
        else Seq.empty
      }
      .toSet
    if (mime.isEmpty) {
      super.genericPredicate(conf)
    } else { record =>
      mime.contains(record.mime) || mime.contains(StringUtil.prefixToSeparator(record.mime, "/"))
    }
  }

  override def warcPredicate(conf: DerivationJobConf): ArchWarcRecord => Boolean = {
    val superFilter = super.warcPredicate(conf)
    val status = conf.params.values.get("status").toSeq.flatMap { status =>
      if (status.isNumber) status.asNumber.flatMap(_.toInt).toSeq
      else if (status.isArray)
        status.asArray.toSeq.flatMap(_.flatMap(_.asNumber).flatMap(_.toInt))
      else Seq.empty
    }
    if (status.isEmpty) {
      superFilter
    } else { warc =>
      superFilter(warc) && {
        status.exists { s =>
          warc.status == s || (s < 100 && (warc.status / 10 == s || (s < 10 && warc.status / 100 == s)))
        }
      }
    }
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    conf.params.values.get("functions").toSeq.flatMap(_.asArray.toSeq.flatten).map { function =>
      ArchiveSparkFlexJob.initFunction(function)
    }
  }

  val adapters: Map[String, ArchArchiveSparkFunctionAdapter[_]] =
    Seq(EntitiesAdapter, Whisper, WhisperText).flatMap { adapter =>
      Iterator(adapter.name -> adapter, adapter.name.toLowerCase -> adapter)
    }.toMap

  private def initFunction[A](
      func: ArchArchiveSparkFunctionAdapter[A],
      cursor: HCursor): EnrichFunc[ArchEnrichRoot[_], A, _] = {
    val dependency =
      cursor.downField("on").focus.map(initFunction).flatMap(func.toDependencyPointer)
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

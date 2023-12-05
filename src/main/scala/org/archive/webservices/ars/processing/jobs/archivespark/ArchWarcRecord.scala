package org.archive.webservices.ars.processing.jobs.archivespark

import io.circe.Json
import org.archive.webservices.archivespark.functions.{HtmlText, StringContent}
import org.archive.webservices.archivespark.model.dataloads.{ByteLoad, DataLoad, TextLoad}
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{EnrichRootCompanion, TypedEnrichRoot}
import org.archive.webservices.archivespark.util.Json.json
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.warc.WarcRecord

import scala.collection.immutable.ListMap

class ArchWarcRecord(warc: WarcRecord) extends ArchEnrichRoot[WarcRecord] with ByteLoad.Root with TextLoad.Root with PlainTextLoad.Root {
  lazy val cdx: CdxRecord = warc.toCdx(0L, handleRevisits = true, handleOthers = true).get

  override def companion: EnrichRootCompanion[ArchWarcRecord] = ArchWarcRecord
  override def get: WarcRecord = warc

  override def metaToJson: Json = {
    json(ListMap[String, Any](
      "surtUrl" -> cdx.surtUrl,
      "timestamp" -> cdx.timestamp,
      "originalUrl" -> cdx.originalUrl,
      "mime" -> cdx.mime,
      "status" -> cdx.status,
      "digest" -> cdx.digest,
      "redirectUrl" -> cdx.redirectUrl,
      "meta" -> cdx.meta,
    ))
  }

  def mime: String = warc.http.flatMap(_.mime).getOrElse("/")
}

object ArchWarcRecord extends EnrichRootCompanion[ArchWarcRecord] {
  implicit def toCdxRecord(record: ArchWarcRecord): CdxRecord = record.cdx
  implicit def toWarcRecord(record: ArchWarcRecord): WarcRecord = record.get

  override def dataLoad[T](load: DataLoad[T]): Option[FieldPointer[ArchWarcRecord, T]] = (load match {
    case ByteLoad => Some(ArchWarcPayload)
    case TextLoad => Some(StringContent)
    case PlainTextLoad => Some(HtmlText)
    case _ => None
  }).map(_.asInstanceOf[FieldPointer[ArchWarcRecord, T]])
}
package org.archive.webservices.ars.processing.jobs.archivespark.base

import io.circe.Json
import org.archive.webservices.archivespark.functions.{HtmlText, StringContent}
import org.archive.webservices.archivespark.model.EnrichRootCompanion
import org.archive.webservices.archivespark.model.dataloads.{ByteLoad, DataLoad, TextLoad}
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.specific.warc.WarcLikeRecord
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaData
import org.archive.webservices.ars.processing.jobs.archivespark.functions.{ArchFileCache, ArchWarcPayload}
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.logging.{Log, LogContext}
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.{File, InputStream}

class ArchWarcRecord(val warc: WarcRecord) extends ArchEnrichRoot[CdxRecord] with WarcLikeRecord {
  implicit private val logContext: LogContext = LogContext(this)

  override def companion: EnrichRootCompanion[ArchWarcRecord] = ArchWarcRecord

  override lazy val get: CdxRecord = {
    warc.toCdx(0L, handleRevisits = true, handleOthers = true).get
  }

  def mime: String = warc.http.flatMap(_.mime).getOrElse("/")

  override lazy val meta: FileMetaData = FileMetaData.fromCdx(get)

  override def metaToJson: Json = meta.toJson

  override def payloadAccess: InputStream = {
    Log.info(s"Accessing ${warc.url.getOrElse("N/A")}...")
    warc.http.map(_.body).getOrElse(warc.payload)
  }

  override def cacheLocal(): File = {
    Log.info(s"Caching ${warc.url.getOrElse("N/A")}...")
    super.cacheLocal()
  }
}

object ArchWarcRecord extends EnrichRootCompanion[ArchWarcRecord] {
  override def dataLoad[T](load: DataLoad[T]): Option[FieldPointer[ArchWarcRecord, T]] =
    (load match {
      case FileLoad => Some(ArchFileCache)
      case ByteLoad => Some(ArchWarcPayload)
      case TextLoad => Some(StringContent)
      case PlainTextLoad => Some(HtmlText)
      case _ => None
    }).map(_.asInstanceOf[FieldPointer[ArchWarcRecord, T]])
}

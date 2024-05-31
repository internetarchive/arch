package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.archive.webservices.archivespark.functions.{HtmlText, StringContent}
import org.archive.webservices.archivespark.model.EnrichRootCompanion
import org.archive.webservices.archivespark.model.dataloads.{ByteLoad, DataLoad, TextLoad}
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.specific.warc.WarcLikeRecord
import org.archive.webservices.ars.model.collections.inputspecs.FileMeta
import org.archive.webservices.ars.processing.jobs.archivespark.functions.{ArchFileCache, ArchWarcPayload}
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.InputStream

class ArchWarcRecord(val warc: WarcRecord)
    extends ArchEnrichRoot[CdxRecord]
    with WarcLikeRecord {
  override def companion: EnrichRootCompanion[ArchWarcRecord] = ArchWarcRecord

  override lazy val get: CdxRecord = warc.toCdx(0L, handleRevisits = true, handleOthers = true).get

  def mime: String = warc.http.flatMap(_.mime).getOrElse("/")

  override def payloadAccess: InputStream = warc.http.map(_.payload).getOrElse(warc.payload)

  override lazy val meta: FileMeta = FileMeta.fromCdx(get)
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

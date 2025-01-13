package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, TypedEnrichable}
import org.archive.webservices.archivespark.specific.warc.functions._
import org.archive.webservices.archivespark.util.Bytes
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchWarcRecord
import org.archive.webservices.sparkling.cdx.CdxRecord

class ArchWarcPayload private (http: Boolean = true)
    extends EnrichFunc[ArchWarcRecord, CdxRecord, Bytes] {
  import WarcPayloadFields._

  val source: FieldPointer[ArchWarcRecord, CdxRecord] =
    FieldPointer.root[ArchWarcRecord, CdxRecord]

  val fields: Seq[String] = {
    if (http) Seq(RecordHeader, HttpStatusLine, HttpHeader, Payload)
    else Seq(RecordHeader, Payload)
  }

  override val defaultField: String = Payload

  override def derive(source: TypedEnrichable[CdxRecord], derivatives: Derivatives): Unit = {
    val record = source.asInstanceOf[ArchWarcRecord]
    val warc = record.warc
    derivatives << warc.headers.toMap
    if (http) {
      for (msg <- warc.http) {
        derivatives << msg.statusLine
        derivatives << msg.headers
        derivatives << record.cachedPayload
      }
    } else {
      derivatives << record.cachedPayload
    }
  }
}

object ArchWarcPayload extends ArchWarcPayload(http = true) {
  def apply(http: Boolean = true) = new ArchWarcPayload(http)
}

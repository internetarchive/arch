package org.archive.webservices.ars.processing.jobs.archivespark

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, TypedEnrichable}
import org.archive.webservices.archivespark.specific.warc.functions._
import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.warc.WarcRecord

class ArchWarcPayload private (http: Boolean = true) extends EnrichFunc[ArchWarcRecord, WarcRecord, Array[Byte]] {
  import WarcPayloadFields._

  val source: FieldPointer[ArchWarcRecord, WarcRecord] = FieldPointer.root[ArchWarcRecord, WarcRecord]

  val fields: Seq[String] = {
    if (http) Seq(RecordHeader, HttpStatusLine, HttpHeader, Payload)
    else Seq(RecordHeader, Payload)
  }

  override val defaultField: String = Payload

  override def derive(source: TypedEnrichable[WarcRecord], derivatives: Derivatives): Unit = {
    val record = source.get
    derivatives << record.headers.toMap
    if (http) {
      for (msg <- record.http) {
        derivatives << msg.statusLine
        derivatives << msg.headers.toMap
        derivatives << IOUtil.bytes(msg.payload)
      }
    } else {
      derivatives << IOUtil.bytes(record.payload)
    }
  }
}

object ArchWarcPayload extends ArchWarcPayload(http = true) {
  def apply(http: Boolean = true) = new ArchWarcPayload(http)
}

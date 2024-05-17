package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.apache.commons.io.input.BoundedInputStream
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, TypedEnrichable}
import org.archive.webservices.archivespark.specific.warc.functions._
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchWarcRecord
import org.archive.webservices.ars.util.HttpUtil
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.io.{CleanupInputStream, IOUtil}

class ArchWarcPayload private (http: Boolean = true)
    extends EnrichFunc[ArchWarcRecord, CdxRecord, Array[Byte]] {
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
        val in = record.localFileCache.getOrElse(msg.payload)
        val bounded = new BoundedInputStream(in, HttpUtil.MaxContentLength)
        derivatives << IOUtil.bytes(new CleanupInputStream(bounded, in.close))
      }
    } else {
      val in = record.localFileCache.getOrElse(warc.payload)
      val bounded = new BoundedInputStream(in, ArchFileBytes.MaxContentLength)
      derivatives << IOUtil.bytes(new CleanupInputStream(bounded, in.close))
    }
  }
}

object ArchWarcPayload extends ArchWarcPayload(http = true) {
  def apply(http: Boolean = true) = new ArchWarcPayload(http)
}

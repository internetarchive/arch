package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichRoot, TypedEnrichable}
import org.archive.webservices.archivespark.util.Bytes
import org.archive.webservices.ars.model.collections.inputspecs.FileRecord
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.sparkling._

import scala.util.Try

object ArchFileBytes
    extends EnrichFunc.Root[EnrichRoot with LocalFileCache, FileRecord, Bytes] {
  val MaxContentLength: Long = 1.mb

  val source: FieldPointer[EnrichRoot with LocalFileCache, FileRecord] = FieldPointer.root

  val fields: Seq[String] = Seq("bytes")

  override def derive(source: TypedEnrichable[FileRecord], derivatives: Derivatives): Unit = {
    Try(source.root.asInstanceOf[LocalFileCache].cachedPayload).toOption
      .getOrElse(Bytes(source.get.access))
  }
}

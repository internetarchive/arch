package org.archive.webservices.ars.processing.jobs.archivespark

import org.apache.commons.io.input.BoundedInputStream
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, TypedEnrichable}
import org.archive.webservices.ars.model.collections.inputspecs.FileRecord
import org.archive.webservices.sparkling.io.{CleanupInputStream, IOUtil}
import org.archive.webservices.sparkling._

object ArchFileBytes extends EnrichFunc[ArchFileRecord, FileRecord, Array[Byte]] {
  val MaxContentLength: Long = 1.mb

  val source: FieldPointer[ArchFileRecord, FileRecord] = FieldPointer.root[ArchFileRecord, FileRecord]
  val fields: Seq[String] = Seq("bytes")
  override val defaultField: String = "bytes"
  override def derive(source: TypedEnrichable[FileRecord], derivatives: Derivatives): Unit = {
    val in = source.get.access
    val bounded = new BoundedInputStream(in, MaxContentLength)
    derivatives << IOUtil.bytes(new CleanupInputStream(bounded, in.close))
  }
}
package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.apache.commons.io.input.BoundedInputStream
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, TypedEnrichRoot, TypedEnrichable}
import org.archive.webservices.ars.model.collections.inputspecs.FileRecord
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.io.{CleanupInputStream, IOUtil}

import scala.util.Try

object ArchFileBytes
    extends EnrichFunc[TypedEnrichRoot[FileRecord] with LocalFileCache, FileRecord, Array[Byte]] {
  val MaxContentLength: Long = 1.mb

  val source: FieldPointer[TypedEnrichRoot[FileRecord] with LocalFileCache, FileRecord] =
    FieldPointer.root[TypedEnrichRoot[FileRecord] with LocalFileCache, FileRecord]

  val fields: Seq[String] = Seq("bytes")

  override def derive(source: TypedEnrichable[FileRecord], derivatives: Derivatives): Unit = {
    val in = Try(source.asInstanceOf[LocalFileCache].localFileCache).toOption.flatten
      .getOrElse(source.get.access)
    val bounded = new BoundedInputStream(in, MaxContentLength)
    derivatives << IOUtil.bytes(new CleanupInputStream(bounded, in.close))
  }
}

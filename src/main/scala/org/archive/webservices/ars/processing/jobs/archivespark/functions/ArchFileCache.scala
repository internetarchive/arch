package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichable}
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache

object ArchFileCache extends EnrichFunc[EnrichRoot with LocalFileCache, Any, String] {
  val source: FieldPointer[EnrichRoot with LocalFileCache, Any] = FieldPointer(Seq.empty)
  val fields: Seq[String] = Seq("filePath")
  override val isTransparent: Boolean = true
  override def derive(source: TypedEnrichable[Any], derivatives: Derivatives): Unit = {
    derivatives << source.asInstanceOf[LocalFileCache].cacheLocal().getPath
  }
}

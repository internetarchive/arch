package org.archive.webservices.ars.processing.jobs.archivespark.base

import io.circe.Json
import org.archive.webservices.archivespark.functions.StringContent
import org.archive.webservices.archivespark.model.EnrichRootCompanion
import org.archive.webservices.archivespark.model.dataloads.{ByteLoad, DataLoad, TextLoad}
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.util.Json.json
import org.archive.webservices.ars.model.collections.inputspecs.FileRecord
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaData
import org.archive.webservices.ars.processing.jobs.archivespark.functions.{ArchFileBytes, ArchFileCache}
import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.logging.{Log, LogContext}

import java.io.{File, InputStream}
import scala.collection.immutable.ListMap
import scala.util.Try

class ArchFileRecord(record: FileRecord) extends ArchEnrichRoot[FileRecord] {
  implicit private val logContext: LogContext = LogContext(this)

  override def companion: EnrichRootCompanion[ArchFileRecord] = ArchFileRecord
  override def get: FileRecord = record

  override def metaToJson: Json = {
    json(
      ListMap[String, Any](
        "filename" -> record.filename,
        "mime" -> Try(record.mime).fold("Error: " + _.getMessage, identity),
        "path" -> Try(record.path).fold("Error: " + _.getMessage, identity)))
  }

  def mime: String = record.mime

  def meta: FileMetaData = record.meta

  override def payloadAccess: InputStream = {
    Log.info(s"Accessing ${record.filename}...")
    IOUtil.supportMark(record.access)
  }

  override def cacheLocal(): File = {
    Log.info(s"Caching ${record.filename}...")
    super.cacheLocal()
  }
}

object ArchFileRecord extends EnrichRootCompanion[ArchFileRecord] {
  override def dataLoad[T](load: DataLoad[T]): Option[FieldPointer[ArchFileRecord, T]] =
    (load match {
      case FileLoad => Some(ArchFileCache)
      case ByteLoad => Some(ArchFileBytes)
      case TextLoad | PlainTextLoad => Some(StringContent)
      case _ => None
    }).map(_.asInstanceOf[FieldPointer[ArchFileRecord, T]])
}

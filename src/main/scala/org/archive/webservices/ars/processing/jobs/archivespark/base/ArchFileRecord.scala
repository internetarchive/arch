package org.archive.webservices.ars.processing.jobs.archivespark.base

import io.circe.Json
import org.archive.webservices.archivespark.functions.StringContent
import org.archive.webservices.archivespark.model.EnrichRootCompanion
import org.archive.webservices.archivespark.model.dataloads.{ByteLoad, DataLoad, TextLoad}
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.util.Json.json
import org.archive.webservices.ars.model.collections.inputspecs.FileRecord

import scala.collection.immutable.ListMap
import scala.util.Try

class ArchFileRecord(record: FileRecord) extends ArchEnrichRoot[FileRecord] with ByteLoad.Root with TextLoad.Root with PlainTextLoad.Root {
  override def companion: EnrichRootCompanion[ArchFileRecord] = ArchFileRecord
  override def get: FileRecord = record

  override def metaToJson: Json = {
    json(ListMap[String, Any](
      "filename" -> record.filename,
      "mime" -> Try(record.mime).fold("Error: " + _.getMessage, identity),
      "path" -> Try(record.path).fold("Error: " + _.getMessage, identity)
    ))
  }

  def mime: String = record.mime
}

object ArchFileRecord extends EnrichRootCompanion[ArchFileRecord] {
  override def dataLoad[T](load: DataLoad[T]): Option[FieldPointer[ArchFileRecord, T]] = (load match {
    case ByteLoad => Some(ArchFileBytes)
    case TextLoad | PlainTextLoad => Some(StringContent)
    case _ => None
  }).map(_.asInstanceOf[FieldPointer[ArchFileRecord, T]])
}
package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.{FileAccessContext, FilePointer, RandomFileAccess}
import org.archive.webservices.ars.model.ArchConf

import java.io.InputStream
import java.net.URL

class PetaboxFileRecordFactory private () extends FileRecordFactory {
  def companion = PetaboxFileRecordFactory

  class PetaboxFileRecord private[PetaboxFileRecordFactory] (
      override val filePath: String,
      val mime: String,
      val meta: FileMeta)
      extends FileRecord {
    override def access: InputStream = accessFile(filePath, resolve = false)
    override def pointer: FilePointer = new FilePointer(RandomFileAccess.PetaboxPrefix + ":" + filePath, filename)
  }

  override def get(file: String, mime: String, meta: FileMeta): FileRecord = {
    new PetaboxFileRecord(file, mime, meta)
  }

  override def accessFile(
      file: String,
      resolve: Boolean,
      accessContext: FileAccessContext = accessContext): InputStream = {
    new URL(ArchConf.iaBaseUrl + s"/serve/").openStream
  }
}

object PetaboxFileRecordFactory extends FileFactoryCompanion {
  val dataSourceType: String = "petabox"

  def apply(spec: InputSpec): PetaboxFileRecordFactory = new PetaboxFileRecordFactory()
}

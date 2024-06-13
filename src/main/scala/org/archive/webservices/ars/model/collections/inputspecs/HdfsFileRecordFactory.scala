package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.FileAccessContext
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaData
import org.archive.webservices.sparkling.io.HdfsIO

import java.io.{FileNotFoundException, InputStream}

class HdfsFileRecordFactory private (excludeSuffix: Option[String] = None)
    extends FileRecordFactory {
  def companion = HdfsFileRecordFactory

  class HdfsFileRecord private[HdfsFileRecordFactory] (
      file: String,
      val mime: String,
      val meta: FileMetaData)
      extends FileRecord {
    override lazy val filePath: String = locateFile(file)
    override def access: InputStream = accessFile(filePath, resolve = false)
  }

  override def get(file: String, mime: String, meta: FileMetaData): FileRecord = {
    new HdfsFileRecord(file, mime, meta)
  }

  override def accessFile(
      file: String,
      resolve: Boolean,
      accessContext: FileAccessContext = accessContext): InputStream = {
    accessContext.hdfsIO.open(if (resolve) locateFile(file) else file)
  }

  def locateFile(filePath: String): String = {
    if (filePath.contains("*")) {
      val files = HdfsIO.files(filePath, recursive = false)
      val filtered =
        if (excludeSuffix.isEmpty) files else files.filter(!_.endsWith(excludeSuffix.get))
      if (filtered.isEmpty) throw new FileNotFoundException()
      filtered.next
    } else filePath
  }
}

object HdfsFileRecordFactory extends FileFactoryCompanion {
  val dataSourceType: String = "hdfs"

  def apply(spec: InputSpec): HdfsFileRecordFactory = new HdfsFileRecordFactory(
    spec.str("meta-suffix"))

  def apply(): HdfsFileRecordFactory = new HdfsFileRecordFactory()
}

package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.CollectionAccessContext
import org.archive.webservices.sparkling.io.HdfsIO

import java.io.{FileNotFoundException, InputStream}

class HdfsFileRecordFactory(excludeSuffix: Option[String]) extends FileRecordFactory {
  class HdfsFileRecord private[HdfsFileRecordFactory] (
      filePath: String,
      val mime: String,
      val meta: FileMeta)
      extends FileRecord {
    private lazy val resolvedPath = locateFile(filePath)

    override lazy val path: String = {
      val slashIdx = resolvedPath.lastIndexOf('/')
      if (slashIdx < 0) "" else resolvedPath.take(slashIdx)
    }

    override lazy val filename: String = resolvedPath.split('/').last

    override def access: InputStream = accessFile(resolvedPath, resolve = false)
  }

  override def get(path: String, mime: String, meta: FileMeta): FileRecord =
    new HdfsFileRecord(path, mime, meta)

  override def accessFile(
      filePath: String,
      resolve: Boolean,
      accessContext: CollectionAccessContext = accessContext): InputStream = {
    accessContext.hdfsIO.open(if (resolve) locateFile(filePath) else filePath)
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

object HdfsFileRecordFactory {
  def apply(spec: InputSpec): HdfsFileRecordFactory = new HdfsFileRecordFactory(
    spec.str("meta-suffix"))
}
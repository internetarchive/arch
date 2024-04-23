package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.FilePointer

import java.io.InputStream

trait FileRecord {
  def filename: String = filePath.split('/').last
  def path: String = {
    val slashIdx = filePath.lastIndexOf('/')
    if (slashIdx < 0) "" else filePath.take(slashIdx)
  }
  def filePath: String = FileRecordFactory.filePath(path, filename)
  def mime: String
  def meta: FileMeta
  def access: InputStream
  def pointer: FilePointer = FilePointer(filePath, filename)

  def withAccess(in: InputStream): FileRecord = {
    val origin = this
    new FileRecord {
      override def filename: String = origin.filename
      override def mime: String = origin.mime
      override def path: String = origin.path
      override def meta: FileMeta = origin.meta
      override def access: InputStream = in
    }
  }
}

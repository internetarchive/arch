package org.archive.webservices.ars.model.collections.inputspecs

import java.io.InputStream

trait FileRecord {
  def filename: String
  def mime: String
  def path: String
  def access: InputStream
  def meta: FileMeta
  def filePath: String = FileRecordFactory.filePath(path, filename)
  def pointer: FilePointer = FilePointer(filePath, filename)
}

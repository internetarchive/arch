package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.WebArchiveLoader
import org.archive.webservices.ars.model.collections.inputspecs.meta.{FileMetaData, FileMetaField}
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.io.IteratorInputStream

import java.io.InputStream

class InMemoryCdxFileRecord (override val filePath: String, records: Iterator[CdxRecord]) extends FileRecord {
  override def mime: String = WebArchiveLoader.CdxMime

  override def meta: FileMetaData = FileMetaData(
    FileMetaField("path", filePath),
    FileMetaField("mime", mime)
  )

  override def access: InputStream = new IteratorInputStream[CdxRecord](records, r => (r.toCdxString + "\n").getBytes)
}

object InMemoryCdxFileRecord {
  def apply(partition: Int, records: Iterator[CdxRecord]): InMemoryCdxFileRecord = {
    new InMemoryCdxFileRecord(s"partition-$partition.cdx.gz", records)
  }
}
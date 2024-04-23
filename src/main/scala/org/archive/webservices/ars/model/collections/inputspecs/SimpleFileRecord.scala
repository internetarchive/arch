package org.archive.webservices.ars.model.collections.inputspecs
import java.io.InputStream

class SimpleFileRecord(
    val mime: String,
    val access: InputStream)
    extends FileRecord {
  override def meta: FileMeta = FileMeta.empty
}

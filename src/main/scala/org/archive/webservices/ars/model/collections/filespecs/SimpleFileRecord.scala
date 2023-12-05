package org.archive.webservices.ars.model.collections.filespecs
import java.io.InputStream

class SimpleFileRecord (
  val filename: String,
  val mime: String,
  val path: String,
  val access: InputStream
) extends FileRecord
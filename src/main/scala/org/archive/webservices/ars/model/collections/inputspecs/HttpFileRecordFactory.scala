package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.CollectionAccessContext

import java.io.InputStream
import java.net.URL

class HttpFileRecordFactory(location: String)
    extends FileRecordFactory {
  class HttpFileRecord private[HttpFileRecordFactory](
      val filename: String,
      val mime: String,
      val meta: FileMeta)
      extends FileRecord {
    override lazy val path: String = locateFile(filename)
    override def access: InputStream = accessFile(filename, resolve = false)
  }

  override def get(filename: String, mime: String, meta: FileMeta): FileRecord =
    new HttpFileRecord(filename, mime, meta)

  def accessFile(
      filename: String,
      resolve: Boolean = true,
      accessContext: CollectionAccessContext): InputStream = {
    val url = if (resolve) locateFile(filename) + "/" + filename else filename
    println(s"Reading $url...")
    new URL(url).openStream
  }

  def locateFile(filename: String): String = location
}

object HttpFileRecordFactory {
  def apply(spec: InputSpec): HttpFileRecordFactory = {
    spec
      .str("data-location")
      .map(new HttpFileRecordFactory(_))
      .getOrElse {
        throw new RuntimeException("No location URL specified.")
      }
  }
}

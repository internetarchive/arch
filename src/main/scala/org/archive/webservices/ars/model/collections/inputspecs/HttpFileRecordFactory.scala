package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.FileAccessContext

import java.io.InputStream
import java.net.URL

class HttpFileRecordFactory(location: String) extends FileRecordFactory {
  def companion: FileFactoryCompanion = HttpFileRecordFactory

  class HttpFileRecord private[HttpFileRecordFactory] (
      file: String,
      val mime: String,
      val meta: FileMeta)
      extends FileRecord {
    override lazy val filePath: String = locateFile(file)
    override def access: InputStream = accessFile(filePath, resolve = false)
  }

  override def get(file: String, mime: String, meta: FileMeta): FileRecord = {
    new HttpFileRecord(file, mime, meta)
  }

  def accessFile(
      file: String,
      resolve: Boolean = true,
      accessContext: FileAccessContext): InputStream = {
    val url = if (resolve) locateFile(file) else file
    println(s"Reading $url...")
    new URL(url).openStream
  }

  def locateFile(filename: String): String = FileRecordFactory.filePath(location, filename)
}

object HttpFileRecordFactory extends FileFactoryCompanion {
  val dataSourceType: String = "http"

  def apply(spec: InputSpec): HttpFileRecordFactory = {
    spec
      .str(InputSpec.DataLocationKey)
      .map(new HttpFileRecordFactory(_))
      .getOrElse {
        throw new RuntimeException("No location URL specified.")
      }
  }
}

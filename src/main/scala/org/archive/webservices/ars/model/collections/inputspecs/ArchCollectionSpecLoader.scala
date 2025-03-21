package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{FilePointer, WebArchiveLoader}
import org.archive.webservices.ars.model.collections.inputspecs.meta.{FileMetaData, FileMetaField}

import java.io.InputStream

object ArchCollectionSpecLoader extends InputSpecLoader {
  val specType = "collection"

  class WarcFileRecord(file: FilePointer, val in: InputStream)
      extends FileRecord
      with OneTimeAccess {
    override def filename: String = file.filename
    override def mime: String = WebArchiveLoader.WarcMime
    override def path: String =
      file.url.stripSuffix(file.filename).stripSuffix(FilePointer.SourceSeparator)
    override def pointer: FilePointer = file
    override lazy val meta: FileMetaData = FileMetaData(
      FileMetaField("filename", filename),
      FileMetaField("mime", mime),
      FileMetaField("path", path))
  }

  override def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.collection.specifics.loadWarcFiles(spec.inputPath) { rdd =>
      action(rdd.map { case (pointer, in) =>
        new WarcFileRecord(pointer, in)
      })
    }
  }

  override def size(spec: InputSpec): Long = spec.collection.stats.size
}

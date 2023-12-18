package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD

import java.io.InputStream

object ArchCollectionSpecLoader extends InputSpecLoader {
  val WarcMime = "application/warc"

  class WarcFileRecord(file: FilePointer, in: InputStream) extends FileRecord {
    override def filename: String = file.filename
    override def mime: String = WarcMime
    override def path: String = file.url.stripSuffix(file.filename).stripSuffix(FilePointer.SourceSeparator)
    override def pointer: FilePointer = file

    private var accessed = false
    override def access: InputStream = {
      if (!accessed) {
        accessed = true
        in
      } else throw new UnsupportedOperationException("InputStream can only be accessed once.")
    }
  }

  override def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.collection.specifics.loadWarcFiles(spec.inputPath) { rdd =>
      action(rdd.map { case (pointer, in) =>
        new WarcFileRecord(pointer, in)
      })
    }
  }
}

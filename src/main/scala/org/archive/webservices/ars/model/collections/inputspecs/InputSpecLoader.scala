package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.WebArchiveLoader

trait InputSpecLoader {
  def specType: String
  def inputType(spec: InputSpec): Option[String] = None
  def size(spec: InputSpec): Long = spec.get[Long]("size").getOrElse(-1)
  def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R
  def loadSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    loadFilesSpark(spec) { rdd =>
      val filtered = spec.inputType match {
        case InputSpec.InputType.WARC =>
          rdd.filter(_.mime == WebArchiveLoader.WarcMime)
        case InputSpec.InputType.CDX =>
          rdd.filter(_.mime == WebArchiveLoader.CdxMime)
        case _ => rdd
      }
      action(filtered)
    }
  }
}

object InputSpecLoader {
  var loaders: Seq[InputSpecLoader] = Seq(
    DatasetSpecLoader,
    ArchCollectionSpecLoader,
    FileSpecLoader,
    MetaRemoteSpecLoader,
    MetaFilesSpecLoader,
    MultiSpecLoader,
    CdxQuerySpecLoader)

  def get(spec: InputSpec): Option[InputSpecLoader] = {
    loaders.find(_.specType == spec.specType)
  }

  def loadSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.loader.loadSpark(spec)(action)
  }

  def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.loader.loadFilesSpark(spec)(action)
  }

  def size(spec: InputSpec): Long = spec.loader.size(spec)
}

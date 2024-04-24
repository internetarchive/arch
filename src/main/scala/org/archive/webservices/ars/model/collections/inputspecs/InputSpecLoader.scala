package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD

trait InputSpecLoader {
  def specType: String
  def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R
  def size(spec: InputSpec): Long = spec.get[Long]("size").getOrElse(-1)
}

object InputSpecLoader {
  val loaders: Seq[InputSpecLoader] = Seq(
    DatasetSpecLoader,
    ArchCollectionSpecLoader,
    FileSpecLoader,
    MetaRemoteSpecLoader,
    MetaFilesSpecLoader,
    MultiSpecLoader)

  def get(spec: InputSpec): Option[InputSpecLoader] = {
    loaders.find(_.specType == spec.specType)
  }

  def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.loader.load(spec)(action)
  }

  def size(spec: InputSpec): Long = spec.loader.size(spec)
}

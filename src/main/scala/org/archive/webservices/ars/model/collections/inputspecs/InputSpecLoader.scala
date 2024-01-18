package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD

trait InputSpecLoader {
  def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R
}

object InputSpecLoader {
  def get(spec: InputSpec): Option[InputSpecLoader] = spec.specType match {
    case "collection" => Some(ArchCollectionSpecLoader)
    case "meta-remote" => Some(MetaRemoteSpecLoader)
    case "meta-files" => Some(MetaFilesSpecLoader)
    case "multi-specs" => Some(MultiSpecLoader)
    case _ => None
  }

  def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.loader.load(spec)(action)
  }
}
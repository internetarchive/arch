package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD

trait InputSpecLoader {
  def load(spec: InputSpec): RDD[FileRecord]
}

object InputSpecLoader {
  def get(spec: InputSpec): Option[InputSpecLoader] = spec.specType match {
    case "meta-remote" => Some(MetaRemoteSpecLoader)
    case _ => None
  }
}
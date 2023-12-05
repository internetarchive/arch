package org.archive.webservices.ars.model.collections.filespecs

import io.circe.HCursor
import org.apache.spark.rdd.RDD

trait FileSpec {
  def load(spec: HCursor): RDD[FileRecord]
}
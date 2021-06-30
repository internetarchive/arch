package org.archive.webservices.ars.model.collections

import javax.servlet.http.HttpServletRequest
import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.model.ArsCloudCollection

abstract class CollectionSpecifics {
  def inputPath: String
  def getCollection(implicit request: HttpServletRequest): Option[ArsCloudCollection]
  def size(implicit request: HttpServletRequest): Long
  def loadWarcs(inputPath: String): RDD[WarcRecord]
}

object CollectionSpecifics {
  def get(id: String): Option[CollectionSpecifics] = {
    if (id.startsWith(AitCollectionSpecifics.Prefix)) Some(new AitCollectionSpecifics(id))
    else if (id.startsWith(CohortCollectionSpecifics.Prefix))
      Some(new CohortCollectionSpecifics(id))
    else None
  }
}

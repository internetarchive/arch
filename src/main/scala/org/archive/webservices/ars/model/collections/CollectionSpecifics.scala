package org.archive.webservices.ars.model.collections

import java.io.InputStream

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser

abstract class CollectionSpecifics {
  def inputPath: String
  def collection(implicit context: RequestContext = RequestContext.None): Option[ArchCollection]
  def size(implicit context: RequestContext = RequestContext.None): Long
  def seeds(implicit context: RequestContext = RequestContext.None): Int
  def lastCrawlDate(implicit context: RequestContext = RequestContext.None): String
  def loadWarcFiles(inputPath: String): RDD[(String, InputStream)]
}

object CollectionSpecifics {
  def get(id: String, collectionUser: ArchUser = ArchUser.None): Option[CollectionSpecifics] = {
    if (id.startsWith(AitCollectionSpecifics.Prefix)) {
      Some(new AitCollectionSpecifics(id))
    } else if (id.startsWith(SpecialCollectionSpecifics.Prefix)) {
      Some(new SpecialCollectionSpecifics(id))
    } else if (id.startsWith(CustomCollectionSpecifics.Prefix)) {
      Some(new CustomCollectionSpecifics(CustomCollectionSpecifics.id(id, collectionUser)))
    } else None
  }
}

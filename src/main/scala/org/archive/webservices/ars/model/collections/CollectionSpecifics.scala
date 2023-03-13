package org.archive.webservices.ars.model.collections

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser

import java.io.InputStream

abstract class CollectionSpecifics {
  def id: String
  def inputPath: String
  def collection(implicit context: RequestContext = RequestContext.None): Option[ArchCollection]
  def size(implicit context: RequestContext = RequestContext.None): Long
  def seeds(implicit context: RequestContext = RequestContext.None): Int
  def lastCrawlDate(implicit context: RequestContext = RequestContext.None): String
  def loadWarcFiles(inputPath: String): RDD[(String, InputStream)]
  def jobOutPath: String = id

  def globalJobOutPath: String = id
  def cacheId: String = id
}

object CollectionSpecifics {
  def get(id: String, collectionUser: ArchUser = ArchUser.None): Option[CollectionSpecifics] = {
    ArchCollection.prefix(id).map { prefix =>
      val collectionId = ArchCollection.id(id, prefix, collectionUser)
      prefix match {
        case AitCollectionSpecifics.Prefix => new AitCollectionSpecifics(collectionId)
        case SpecialCollectionSpecifics.Prefix => new SpecialCollectionSpecifics(collectionId)
        case CustomCollectionSpecifics.Prefix => new CustomCollectionSpecifics(collectionId)
      }
    }
  }
}

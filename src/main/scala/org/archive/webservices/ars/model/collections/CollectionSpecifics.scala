package org.archive.webservices.ars.model.collections

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
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
  def sourceId: String = id
}

object CollectionSpecifics {
  def get(id: String): Option[CollectionSpecifics] = {
    ArchCollection.prefix(id).map {
      case AitCollectionSpecifics.Prefix => new AitCollectionSpecifics(id)
      case SpecialCollectionSpecifics.Prefix => new SpecialCollectionSpecifics(id)
      case CustomCollectionSpecifics.Prefix => new CustomCollectionSpecifics(id)
    }
  }
}

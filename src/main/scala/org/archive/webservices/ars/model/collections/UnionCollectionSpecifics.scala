package org.archive.webservices.ars.model.collections

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{CollectionAccessContext, CollectionSourcePointer}
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.sparkling.util.RddUtil

import java.io.InputStream

class UnionCollectionSpecifics(val id: String) extends CollectionSpecifics {
  val (userId, collectionId) = ArchCollection.splitIdUserCollection(id.stripPrefix(UnionCollectionSpecifics.Prefix))

  def inputPath: String = ""

  def collection(
      implicit context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    Some(ArchCollection(id, collectionId, public = false, userId.map((_, collectionId)), sourceId))
  }

  def size(implicit context: RequestContext = RequestContext.None): Long = -1

  def seeds(implicit context: RequestContext = RequestContext.None): Int = -1

  def lastCrawlDate(implicit context: RequestContext = RequestContext.None): String = ""

  def loadWarcFiles[R](inputPath: String)(action: RDD[(String, InputStream)] => R): R = {
    def union(rdd: RDD[(String, InputStream)], remaining: Seq[CollectionSpecifics]): R = {
      if (remaining.nonEmpty) {
        val specifics = remaining.head
        specifics.loadWarcFiles(specifics.inputPath) { nextRdd =>
          union(rdd.union(nextRdd), remaining.tail)
        }
      } else action(rdd)
    }
    val sourceIds = inputPath.split(",").map(_.trim).filter(_.nonEmpty).distinct
    union(RddUtil.emptyRDD[(String, InputStream)], sourceIds.flatMap(CollectionSpecifics.get))
  }

  private val collectionSpecifics = scala.collection.mutable.Map.empty[String, Option[CollectionSpecifics]]
  def randomAccess(context: CollectionAccessContext, inputPath: String, pointer: CollectionSourcePointer, initialOffset: Long, positions: Iterator[(Long, Long)]): Iterator[InputStream] = {
    collectionSpecifics.getOrElseUpdate(pointer.sourceId, CollectionSpecifics.get(pointer.sourceId)).toIterator.flatMap { specifics =>
      specifics.randomAccess(context, specifics.inputPath, pointer, initialOffset, positions)
    }
  }

  override def sourceId: String = UnionCollectionSpecifics.Prefix + collectionId
}

object UnionCollectionSpecifics {
  val Prefix = "UNION-"
}

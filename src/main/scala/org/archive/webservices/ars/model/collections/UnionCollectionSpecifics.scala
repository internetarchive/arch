package org.archive.webservices.ars.model.collections

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{CollectionAccessContext, CollectionSourcePointer}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats}
import org.archive.webservices.ars.processing.{DerivationJobInstance, DerivationJobParameters}
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.util.RddUtil

import java.io.InputStream
import scala.reflect.ClassTag

class UnionCollectionSpecifics(val id: String) extends CollectionSpecifics {
  val (userId, collectionId) =
    ArchCollection.splitIdUserCollection(id.stripPrefix(UnionCollectionSpecifics.Prefix))

  def inputPath: String = ""

  def collection(
      implicit context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    Some(
      ArchCollection(
        id,
        collectionId,
        public = false,
        userId.map((_, UnionCollectionSpecifics.Prefix + collectionId)),
        sourceId))
  }

  override def stats(implicit context: RequestContext): ArchCollectionStats =
    ArchCollectionStats.Empty

  override def inputSize(instance: DerivationJobInstance): Long = {
    UnionCollectionSpecifics
      .collections(instance.conf.params)
      .map(_.specifics.inputSize(instance))
      .filter(_ > -1)
      .sum
  }

  private def loadUnion[A: ClassTag, R](
      inputPath: String,
      load: CollectionSpecifics => (RDD[A] => R) => R)(action: RDD[A] => R): R = {
    def union(rdd: RDD[A], remaining: Seq[CollectionSpecifics], numPartitions: Int): R = {
      if (remaining.nonEmpty) {
        load(remaining.head) { nextRdd =>
          union(rdd.union(nextRdd), remaining.tail, nextRdd.getNumPartitions.max(numPartitions))
        }
      } else action(rdd.coalesce(numPartitions))
    }
    val sourceIds = inputPath.split(',').map(_.trim).filter(_.nonEmpty).distinct
    union(RddUtil.emptyRDD[A], sourceIds.flatMap(CollectionSpecifics.get), 0)
  }

  def loadWarcFiles[R](inputPath: String)(action: RDD[(String, InputStream)] => R): R = {
    loadUnion[(String, InputStream), R](inputPath, s => s.loadWarcFiles(s.inputPath))(action)
  }

  override def loadCdx[R](inputPath: String)(action: RDD[CdxRecord] => R): R = {
    loadUnion[CdxRecord, R](inputPath, s => s.loadCdx(s.inputPath))(action)
  }

  private val collectionSpecifics =
    scala.collection.mutable.Map.empty[String, Option[CollectionSpecifics]]
  def randomAccess(
      context: CollectionAccessContext,
      inputPath: String,
      pointer: CollectionSourcePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    collectionSpecifics
      .getOrElseUpdate(pointer.sourceId, CollectionSpecifics.get(pointer.sourceId))
      .map { specifics =>
        specifics.randomAccess(context, specifics.inputPath, pointer, offset, positions)
      }
      .getOrElse(IOUtil.EmptyStream)
  }
}

object UnionCollectionSpecifics {
  val Prefix = "UNION-"

  def collections(params: DerivationJobParameters)(
      implicit context: RequestContext = RequestContext.None): Seq[ArchCollection] = {
    params
      .get[Array[String]]("input")
      .toSeq
      .flatten
      .distinct
      // TODO: Insert the user ID into the collectionId string if necessary. Is this really needed? why? we're not operating on collection outputs here, so the user ID shouldn't matter?
      .map { collectionId =>
        ArchCollection.userCollectionId(collectionId, context.user)
      }
      .sorted
      .flatMap(ArchCollection.get(_))
  }
}

package org.archive.webservices.ars.model.collections

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.inputspecs.FilePointer
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats}
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobParameters}
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.util.RddUtil

import java.io.InputStream
import scala.reflect.ClassTag

class UnionCollectionSpecifics(val id: String)
    extends CollectionSpecifics
    with GenericRandomAccess {
  val (userId, collectionId) =
    ArchCollection.splitIdUserCollection(id.stripPrefix(UnionCollectionSpecifics.Prefix))

  def inputPath: String = ""

  def collection(implicit
      context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    Some(
      ArchCollection(
        id,
        collectionId,
        public = false,
        userId.map((_, UnionCollectionSpecifics.Prefix + collectionId)),
        sourceId))
  }

  override def stats(implicit
      context: RequestContext = RequestContext.None): ArchCollectionStats =
    ArchCollectionStats.Empty

  override def inputSize(conf: DerivationJobConf): Long = {
    UnionCollectionSpecifics
      .collections(conf.params)
      .map(_.specifics.inputSize(conf))
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

  def loadWarcFiles[R](inputPath: String)(action: RDD[(FilePointer, InputStream)] => R): R = {
    loadUnion[(FilePointer, InputStream), R](inputPath, s => s.loadWarcFiles(s.inputPath))(action)
  }

  override def loadCdx[R](inputPath: String)(action: RDD[CdxRecord] => R): R = {
    loadUnion[CdxRecord, R](inputPath, s => s.loadCdx(s.inputPath))(action)
  }
}

object UnionCollectionSpecifics {
  val Prefix = "UNION-"

  def collections(params: DerivationJobParameters)(implicit
      context: RequestContext = RequestContext.None): Seq[ArchCollection] = {
    params
      .get[Array[String]]("input")
      .toSeq
      .flatten
      .distinct
      .sorted
      .flatMap(ArchCollection.get(_))
  }
}

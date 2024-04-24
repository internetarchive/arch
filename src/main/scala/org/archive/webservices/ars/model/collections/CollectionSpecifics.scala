package org.archive.webservices.ars.model.collections

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{CollectionAccessContext, WebArchiveLoader}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.inputspecs.FilePointer
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.sparkling.cdx.CdxRecord

import java.io.InputStream

abstract class CollectionSpecifics {
  def id: String
  def inputPath: String
  def sourceId: String = id

  def collection(implicit context: RequestContext = RequestContext.None): Option[ArchCollection]
  def stats(implicit context: RequestContext = RequestContext.None): ArchCollectionStats
  def inputSize(conf: DerivationJobConf): Long = conf.inputSpec.collection.stats.size
  def loadWarcFiles[R](inputPath: String)(action: RDD[(FilePointer, InputStream)] => R): R

  def loadCdx[R](inputPath: String)(action: RDD[CdxRecord] => R): R = loadWarcFiles(inputPath) {
    rdd =>
      action(WebArchiveLoader.loadCdxFromWarcGzStreams(rdd))
  }

  def randomAccess(
      context: CollectionAccessContext,
      inputPath: String,
      pointer: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream
}

object CollectionSpecifics {
  def get(id: String): Option[CollectionSpecifics] = {
    ArchCollection.prefix(id).map {
      case AitCollectionSpecifics.Prefix => new AitCollectionSpecifics(id)
      case SpecialCollectionSpecifics.Prefix => new SpecialCollectionSpecifics(id)
      case CustomCollectionSpecifics.Prefix => new CustomCollectionSpecifics(id)
      case UnionCollectionSpecifics.Prefix => new UnionCollectionSpecifics(id)
      case FileCollectionSpecifics.Prefix => new FileCollectionSpecifics(id)
    }
  }

  def pointer(sourceId: String, filename: String): FilePointer =
    FilePointer(sourceId + FilePointer.SourceSeparator + filename, filename)
}

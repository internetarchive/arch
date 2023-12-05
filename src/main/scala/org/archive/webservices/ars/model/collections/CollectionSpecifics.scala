package org.archive.webservices.ars.model.collections

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{CollectionAccessContext, CollectionLoader, CollectionSourcePointer}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.filespecs.{FileRecord, SimpleFileRecord}
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats}
import org.archive.webservices.ars.processing.DerivationJobInstance
import org.archive.webservices.sparkling.cdx.CdxRecord
import org.archive.webservices.sparkling.warc.{WarcLoader, WarcRecord}

import java.io.InputStream

abstract class CollectionSpecifics {
  def id: String
  def inputPath: String
  def sourceId: String = id

  def collection(implicit context: RequestContext = RequestContext.None): Option[ArchCollection]
  def stats(implicit context: RequestContext = RequestContext.None): ArchCollectionStats
  def inputSize(instance: DerivationJobInstance): Long = instance.collection.stats.size
  def loadWarcFiles[R](inputPath: String)(action: RDD[(String, InputStream)] => R): R

  def loadCdx[R](inputPath: String)(action: RDD[CdxRecord] => R): R = loadWarcFiles(inputPath) {
    rdd =>
      action(CollectionLoader.loadCdxFromWarcGzStreams(rdd, sourceId))
  }

  def randomAccess(
      context: CollectionAccessContext,
      inputPath: String,
      pointer: CollectionSourcePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream

  def loadFiles[R](inputPath: String)(action: RDD[FileRecord] => R): R = {
    loadWarcFiles(inputPath)(rdd => action(rdd.flatMap { case (filename, in) =>
      WarcLoader.load(in).flatMap { warc =>
        for {
          http <- warc.http
          mime <- http.mime
        } yield {
          new SimpleFileRecord(filename, mime, inputPath, http.payload)
        }
      }
    }))
  }
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
}

package org.archive.webservices.ars.model.collections

import org.archive.webservices.ars.io.{CollectionAccessContext, IOHelper}
import org.archive.webservices.ars.model.collections.inputspecs.FilePointer
import org.archive.webservices.sparkling.io.IOUtil
import scala.collection.mutable

import java.io.InputStream
import java.net.URL

trait GenericRandomAccess {
  private val collectionSpecificsCache = mutable.Map.empty[String, Option[CollectionSpecifics]]

  def randomAccess(
      context: CollectionAccessContext,
      inputPath: String,
      pointer: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    GenericRandomAccess.randomAccess(context, pointer, offset, positions, collectionSpecificsCache)
  }
}

object GenericRandomAccess {
  def randomAccess(
      context: CollectionAccessContext,
      pointer: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)],
      collectionSpecificsCache :mutable.Map[String, Option[CollectionSpecifics]] = mutable.Map.empty): InputStream = {
    if (pointer.isHttpSource) {
      val in = new URL(pointer.url).openStream
      IOUtil.skip(in, offset)
      IOHelper.splitMergeInputStreams(in, positions, buffered = false)
    } else if (pointer.isCollectionSource) {
      collectionSpecificsCache
        .getOrElseUpdate(pointer.source, CollectionSpecifics.get(pointer.source))
        .map { specifics =>
          specifics.randomAccess(context, specifics.inputPath, pointer, offset, positions)
        }
        .getOrElse(IOUtil.EmptyStream)
    } else throw new UnsupportedOperationException()
  }
}
package org.archive.webservices.ars.model.collections

import org.archive.webservices.ars.io.{CollectionAccessContext, CollectionSourcePointer}
import org.archive.webservices.sparkling.io.IOUtil

import java.io.InputStream

trait GenericRandomAccess {
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

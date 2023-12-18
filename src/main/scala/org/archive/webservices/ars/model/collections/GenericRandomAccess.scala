package org.archive.webservices.ars.model.collections

import org.archive.webservices.ars.io.{CollectionAccessContext, IOHelper}
import org.archive.webservices.ars.model.collections.inputspecs.FilePointer
import org.archive.webservices.sparkling.io.IOUtil

import java.io.InputStream
import java.net.URL

trait GenericRandomAccess {
  private val collectionSpecifics =
    scala.collection.mutable.Map.empty[String, Option[CollectionSpecifics]]

  def randomAccess(
                    context: CollectionAccessContext,
                    inputPath: String,
                    pointer: FilePointer,
                    offset: Long,
                    positions: Iterator[(Long, Long)]): InputStream = {
    if (pointer.isHttpSource) {
      val in = new URL(pointer.url).openStream
      IOUtil.skip(in, offset)
      IOHelper.splitMergeInputStreams(in, positions)
    } else if (pointer.isCollectionSource) {
      collectionSpecifics
        .getOrElseUpdate(pointer.source, CollectionSpecifics.get(pointer.source))
        .map { specifics =>
          specifics.randomAccess(context, specifics.inputPath, pointer, offset, positions)
        }
        .getOrElse(IOUtil.EmptyStream)
    } else throw new UnsupportedOperationException()
  }
}

package org.archive.webservices.ars.model.collections

import org.archive.webservices.ars.io.{FileAccessContext, FilePointer, RandomFileAccess}

import java.io.InputStream

trait GenericRandomAccess {
  def randomAccess(
                    context: FileAccessContext,
                    inputPath: String,
                    pointer: FilePointer,
                    offset: Long,
                    positions: Iterator[(Long, Long)]): InputStream =
    RandomFileAccess.access(context, pointer, offset, positions)
}

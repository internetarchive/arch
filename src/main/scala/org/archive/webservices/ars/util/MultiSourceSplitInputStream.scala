package org.archive.webservices.ars.util

import org.archive.webservices.ars.io.MultiSourceInputStream

import java.io.InputStream

class MultiSourceSplitInputStream(
    offset: Long,
    positions: Iterator[(Long, Long)],
    sources: ((Long, Iterator[(Long, Long)]) => Option[InputStream])*) extends MultiSourceInputStream {
  private val preservedPositions = positions.toSeq
  private val sourcesIter = sources.toIterator
  override def nextSource(pos: Long): Option[InputStream] = {
    val shiftedOffset = offset + pos
    val shiftedPositions = preservedPositions.map{case (o, l) => (o - pos, l)}.filter(_._1 >= 0)
    val streams = sourcesIter.flatMap(_(shiftedOffset, shiftedPositions.toIterator))
    if (streams.hasNext) Some(streams.next) else None
  }
}

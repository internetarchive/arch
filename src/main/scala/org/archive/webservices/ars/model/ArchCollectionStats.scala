package org.archive.webservices.ars.model

case class ArchCollectionStats(size: Long, seeds: Long = -1, lastCrawlDate: String = "")

object ArchCollectionStats {
  val Empty: ArchCollectionStats = ArchCollectionStats(-1)
}

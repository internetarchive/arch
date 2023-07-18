package org.archive.webservices.ars.model.api

import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionInfo}
import org.archive.webservices.ars.util.FormatUtil

case class Collection(
  id: String,
  name: String,
  public: Boolean,
  size: String,
  sortSize: Long,
  seeds: Long,
  lastCrawlDate: Option[String],
  lastJobId: Option[String],
  lastJobSample: Option[java.lang.Boolean],
  lastJobName: Option[String],
  lastJobTime: Option[String]) extends ApiResponseObject[Collection]

object Collection {
  def apply(collection: ArchCollection)(implicit context: RequestContext): Collection = {
    val info = ArchCollectionInfo.get(collection.id)
    Collection(
      id = collection.userUrlId(context.user.id),
      name = collection.name,
      public = collection.public,
      size = FormatUtil.formatBytes(collection.stats.size),
      sortSize = collection.stats.size,
      seeds = collection.stats.seeds,
      lastCrawlDate = Option(collection.stats.lastCrawlDate).filter(_.nonEmpty),
      lastJobId = info.flatMap(_.lastJobId),
      lastJobSample = info.flatMap(_.lastJobSample).map(Boolean.box),
      lastJobName = info.flatMap(_.lastJobName),
      lastJobTime = info.flatMap(_.lastJobTime).map(FormatUtil.instantTimeString))
  }
}

package org.archive.webservices.ars.model.api

import io.circe.Json
import io.circe.syntax._
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.CustomCollectionSpecifics
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionInfo}
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery
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
    lastJobTime: Option[String],
    params: Option[Json])
    extends ApiResponseObject[Collection]

object Collection {
  private def params(collection: ArchCollection): Option[Json] =
    if (collection.specifics.isInstanceOf[CustomCollectionSpecifics])
      Some(
        UserDefinedQuery
          .parseInfo(CustomCollectionSpecifics.path(collection.id).get)
          .get
          .top
          .get
          .asObject
          .get
          .filterKeys(k => k != "name" && k != "size")
          .asJson)
    else None

  def apply(collection: ArchCollection)(implicit context: RequestContext): Collection = {
    val info = ArchCollectionInfo.get(collection.id)
    Collection(
      id = collection.id,
      name = collection.name,
      public = collection.public,
      size = FormatUtil.formatBytes(collection.stats.size),
      sortSize = collection.stats.size,
      seeds = collection.stats.seeds,
      lastCrawlDate = Option(collection.stats.lastCrawlDate).filter(_.nonEmpty),
      lastJobId = info.flatMap(_.lastJobId),
      lastJobSample = info.flatMap(_.lastJobSample).map(Boolean.box),
      lastJobName = info.flatMap(_.lastJobName),
      lastJobTime = info.flatMap(_.lastJobTime).map(FormatUtil.instantTimeString),
      params = params(collection))
  }
}

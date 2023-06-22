package org.archive.webservices.ars.model.api

import io.circe._
import io.circe.syntax._

import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionInfo}
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.util.FormatUtil

class Collection(collection: ArchCollection, user: ArchUser) {
  private val _info = ArchCollectionInfo.get(collection.id)

  val id = collection.userUrlId(user.id)
  val name = collection.name
  val public = collection.public
  val size = FormatUtil.formatBytes(collection.stats.size)
  val sortSize = collection.stats.size
  val seeds = collection.stats.seeds
  val lastCrawlDate = Option(collection.stats.lastCrawlDate).filter(_.nonEmpty)
  val lastJobId = _info.flatMap(_.lastJobId)
  val lastJobSample = _info.flatMap(_.lastJobSample)
  val lastJobName = _info.flatMap(_.lastJobName)
  val lastJobTime = _info.flatMap(_.lastJobTime).map(FormatUtil.instantTimeString)
}

object Collection {
  private def encode(c: Collection): Json = {
    Json.obj(
      "id" -> c.id.asJson,
      "name" -> c.name.asJson,
      "public" -> c.public.asJson,
      "size" -> c.size.asJson,
      "sortSize" -> c.sortSize.asJson,
      "seeds" -> c.seeds.asJson,
      "lastCrawlDate" -> c.lastCrawlDate.asJson,
      "lastJobId" -> c.lastJobId.asJson,
      "lastJobSample" -> c.lastJobSample.asJson,
      "lastJobName" -> c.lastJobName.asJson,
      "lastJobTime" -> c.lastJobTime.asJson)
  }

  implicit val encodeCollection: Encoder[Collection] = encode
}

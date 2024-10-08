package org.archive.webservices.ars.model.collections

import io.circe.{HCursor, Json, JsonObject, parser}
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.io.{FileAccessContext, FilePointer, WebArchiveLoader}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats, ArchConf}
import org.archive.webservices.ars.util.CacheUtil
import org.archive.webservices.sparkling.util.StringUtil

import java.io.InputStream
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Try

class AitCollectionSpecifics(val id: String) extends CollectionSpecifics {
  val (userId, collectionId) =
    ArchCollection.splitIdUserCollection(id.stripPrefix(AitCollectionSpecifics.Prefix))
  val aitId: Int = collectionId.toInt

  private def foreignAccess(implicit context: RequestContext): Boolean = {
    context.isInternal || (context.isAdmin && context.loggedIn.aitUser.isEmpty) || context.loggedInOpt
      .exists { u =>
        AitCollectionSpecifics.foreignCollectionIds(u).contains(aitId)
      }
  }

  def inputPath: String =
    ArchConf.aitCollectionPath + s"/$aitId/" + ArchConf.aitCollectionWarcDir

  def collection(implicit context: RequestContext = RequestContext.None): Option[ArchCollection] =
    AitCollectionSpecifics
      .fetchCollections(Seq(aitId), userId.map(ArchUser.get(_).get), foreignAccess)
      .headOption

  override def stats(implicit
      context: RequestContext = RequestContext.None): ArchCollectionStats =
    collection
      .map(AitCollectionSpecifics.getAitId)
      .flatMap(cid =>
        AitCollectionSpecifics
          .getCollectionStatsPair(
            cid,
            if (context.user != ArchUser.None) Some(context.user) else None)
          .map(_._2))
      .getOrElse(ArchCollectionStats.Empty)

  def loadWarcFiles[R](inputPath: String)(action: RDD[(FilePointer, InputStream)] => R): R = {
    val sourceId = this.sourceId
    WebArchiveLoader.loadAitWarcFiles(aitId, inputPath, sourceId) { rdd =>
      action(rdd.map { case (filename, in) =>
        (CollectionSpecifics.pointer(sourceId, filename), in)
      })
    }
  }

  def randomAccess(
      context: FileAccessContext,
      inputPath: String,
      pointer: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    WebArchiveLoader.randomAccessAit(
      context,
      sourceId,
      inputPath + "/" + pointer.filename,
      offset,
      positions)
  }

  override def sourceId: String = AitCollectionSpecifics.Prefix + collectionId
}

object AitCollectionSpecifics {
  val Prefix = "ARCHIVEIT-"

  def getAitId(c: ArchCollection): Int = c.sourceId.stripPrefix(Prefix).toInt

  private val cacheTTL: Duration = 10.minutes

  private type UserCollectionIds = Seq[Int]
  private type CollectionStatsPair = (ArchCollection, ArchCollectionStats)

  private def userCollectionIdsCacheKey(aitUserId: Int): String =
    s"AitCollectionSpecifics:ucids:${aitUserId}"

  private def collectionStatsCacheKey(collectionId: Int, user: Option[ArchUser]): String =
    user
      .map(u => s"AitCollectionSpecifics:cs:${u.id}:${collectionId}")
      .getOrElse(s"AitCollectionSpecifics:cs:${collectionId}")

  private def putUserCollectionIds(
      aitUserId: Int,
      collectionIds: UserCollectionIds): UserCollectionIds =
    CacheUtil.put[UserCollectionIds](
      userCollectionIdsCacheKey(aitUserId),
      collectionIds,
      ttl = Some(cacheTTL))

  private def getUserCollectionIds(aitUserId: Int): Option[UserCollectionIds] =
    CacheUtil.get[UserCollectionIds](userCollectionIdsCacheKey(aitUserId))

  private def putCollectionStatsPair(
      aitId: Int,
      user: Option[ArchUser],
      pair: CollectionStatsPair): CollectionStatsPair =
    CacheUtil
      .put[CollectionStatsPair](collectionStatsCacheKey(aitId, user), pair, ttl = Some(cacheTTL))

  private def getCollectionStatsPair(
      aitId: Int,
      user: Option[ArchUser]): Option[CollectionStatsPair] =
    CacheUtil.get[CollectionStatsPair](collectionStatsCacheKey(aitId, user))

  private var _foreignCollectionsCursor: Option[HCursor] = None
  private def foreignCollectionsCursor: HCursor = _foreignCollectionsCursor.getOrElse {
    _foreignCollectionsCursor = Some(Try {
      val source = Source.fromFile("data/ait-collections.json", "utf-8")
      try {
        parser.parse(source.mkString).right.get.hcursor
      } finally {
        source.close()
      }
    }.getOrElse(Json.fromJsonObject(JsonObject.empty).hcursor))
    _foreignCollectionsCursor.get
  }

  private def foreignCollectionIds(user: ArchUser): Seq[Int] = {
    foreignCollectionsCursor
      .downField(user.datafileKey)
      .values
      .toSeq
      .flatten
      .flatMap(_.asNumber.flatMap(_.toInt))
  }

  private def parseCollections(
      json: Iterator[Json],
      user: Option[ArchUser]): Seq[(ArchCollection, ArchCollectionStats)] = {
    json
      .map(_.hcursor)
      .flatMap { c =>
        c.get[Int]("id").right.toOption.map { aitId =>
          val collectionId = StringUtil.padNum(aitId, 5)
          (
            ArchCollection(
              ArchCollection.prependUserId(collectionId, user.map(_.id), Prefix),
              c.get[String]("name").right.getOrElse(Prefix + collectionId),
              c.get[Boolean]("publicly_visible").right.getOrElse(false),
              user.map(u => (u.id, Prefix + collectionId)),
              Prefix + collectionId),
            ArchCollectionStats(
              c.get[Long]("total_warc_bytes").right.getOrElse(0L),
              (
                c.get[Long]("num_active_seeds").right.getOrElse(0L)
                  + c.get[Long]("num_inactive_seeds").right.getOrElse(0L)
              ),
              c.get[String]("last_crawl_date").right.getOrElse("")))
        }
      }
      .toSeq
  }

  private def userCollectionIds(user: ArchUser): UserCollectionIds =
    user.aitUser.map(_.id).toSeq.flatMap { aitUserId =>
      getUserCollectionIds(aitUserId)
        .getOrElse(
          putUserCollectionIds(
            aitUserId,
            Ait
              .getJson(
                s"/api/collection?limit=100&account=${aitUserId}&pluck=id",
                basicAuth = ArchConf.foreignAitAuthHeader)(c => c.as[UserCollectionIds].toOption)
              .getOrElse(Seq.empty)))
    }

  private def fetchCollections(
      aitIds: Seq[Int],
      user: Option[ArchUser],
      useForeignAccess: Boolean = false): Seq[ArchCollection] =
    synchronized {
      val cachedCollections =
        aitIds.flatMap(aitId => getCollectionStatsPair(aitId, user).map(_._1))
      val uncachedIds = aitIds.toSet.diff(cachedCollections.map(getAitId).toSet)
      cachedCollections ++ {
        if (uncachedIds.isEmpty) Seq.empty
        else {
          Ait
            .getJson(
              s"/api/collection?id__in=${uncachedIds.mkString(",")}",
              basicAuth = if (useForeignAccess) ArchConf.foreignAitAuthHeader else None)(c =>
              Some(parseCollections(c.values.toIterator.flatten, user)))
            .getOrElse(Seq.empty)
            // Cache these collections and stats and return the collections.
            .map(p => {
              putCollectionStatsPair(getAitId(p._1), user, p)
              p._1
            })
        }
      }
    }

  def invalidateData(): Unit = _foreignCollectionsCursor = None

  def userCollections(user: ArchUser): Seq[ArchCollection] = synchronized {
    fetchCollections(userCollectionIds(user), Some(user), true)
  }

  def foreignUserCollections(user: ArchUser): Seq[ArchCollection] = synchronized {
    fetchCollections(foreignCollectionIds(user), Some(user), true)
  }
}

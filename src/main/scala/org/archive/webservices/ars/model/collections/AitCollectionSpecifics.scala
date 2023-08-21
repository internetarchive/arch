package org.archive.webservices.ars.model.collections

import io.circe.{HCursor, Json, JsonObject, parser}
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.io.{
  CollectionAccessContext,
  CollectionLoader,
  CollectionSourcePointer
}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats, ArchConf}
import org.archive.webservices.ars.util.FuturesUtil.waitAll
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.util.StringUtil

import java.io.InputStream
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.{Success, Try}

class AitCollectionSpecifics(val id: String) extends CollectionSpecifics {
  val (userId, collectionId) =
    ArchCollection.splitIdUserCollection(id.stripPrefix(AitCollectionSpecifics.Prefix))
  val aitId: Int = collectionId.toInt
  private var collectionStatsOpt: Option[(ArchCollection, ArchCollectionStats)] = None

  private def foreignAccess(implicit context: RequestContext = RequestContext.None): Boolean = {
    context.isInternal || (context.isAdmin && context.loggedIn.aitUser.isEmpty) || context.loggedInOpt
      .exists { u =>
        AitCollectionSpecifics.foreignCollectionIds(u).contains(aitId)
      }
  }

  def inputPath: String =
    ArchConf.aitCollectionPath + s"/$aitId/" + ArchConf.aitCollectionWarcDir

  private def fetchCollection(implicit context: RequestContext = RequestContext.None)
      : Option[(ArchCollection, ArchCollectionStats)] = {
    Ait
      .getJson(
        s"/api/collection?id=$aitId"
          + "&annotate__count=seed__id"
          + "&seed__deleted__in=false,null"
          + "&annotate__max=crawljobrun__processing_end_date"
          + "&crawljobrun__crawl_job__type__in=null,ANNUAL,BIMONTHLY,CRAWL_SELECTED_SEEDS,DAILY,MISSING_URLS_PATCH_CRAWL,MONTHLY,ONE_TIME,QUARTERLY,SEMIANNUAL,TEST_SAVED,TWELVE_HOURS,WEEKLY,TEST",
        basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(c => {
        val stats = c.values.toSeq.head
          .map(_.hcursor)
          .map { collection =>
            ArchCollectionStats(
              collection.get[Long]("total_warc_bytes").right.getOrElse(0L),
              collection.get[Long]("seed__id").right.getOrElse(-1L),
              collection.get[String]("crawljobrun__processing_end_date").right.getOrElse(""))
          }
          .head
        Some(
          AitCollectionSpecifics.parseCollections(c.values.toIterator.flatten, userId).head,
          stats)
      })
      .toOption
  }

  def ensureCollectionStats[R](get: ((ArchCollection, ArchCollectionStats)) => R)(implicit
      context: RequestContext = RequestContext.None): Option[R] = {
    if (collectionStatsOpt.isEmpty) collectionStatsOpt = fetchCollection
    collectionStatsOpt.map(get)
  }

  def collection(implicit context: RequestContext = RequestContext.None): Option[ArchCollection] =
    ensureCollectionStats {
      _._1
    }

  override def stats(implicit
      context: RequestContext = RequestContext.None): ArchCollectionStats =
    ensureCollectionStats {
      _._2
    }.getOrElse(ArchCollectionStats.Empty)

  def loadWarcFiles[R](inputPath: String)(action: RDD[(String, InputStream)] => R): R =
    CollectionLoader.loadAitWarcFiles(aitId, inputPath, sourceId)(action)

  def randomAccess(
      context: CollectionAccessContext,
      inputPath: String,
      pointer: CollectionSourcePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    CollectionLoader.randomAccessAit(
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

  def invalidateData(): Unit = _foreignCollectionsCursor = None

  def foreignCollectionIds(user: ArchUser): Seq[Int] = {
    foreignCollectionsCursor
      .downField(user.id)
      .values
      .toSeq
      .flatten
      .flatMap(_.asNumber.flatMap(_.toInt))
  }

  def parseCollections(json: Iterator[Json], userId: Option[String]): Seq[ArchCollection] = {
    json
      .map(_.hcursor)
      .flatMap { c =>
        c.get[Int]("id").right.toOption.map { aitId =>
          val collectionId = StringUtil.padNum(aitId, 5)
          ArchCollection(
            ArchCollection.prependUserId(collectionId, userId, Prefix),
            c.get[String]("name").right.getOrElse(Prefix + collectionId),
            c.get[Boolean]("publicly_visible").right.getOrElse(false),
            userId.map((_, Prefix + collectionId)),
            Prefix + collectionId)
        }
      }
      .toSeq
  }

  def userCollections(user: ArchUser)(implicit
      context: RequestContext = RequestContext.None): Seq[ArchCollection] = {
    val foreignAccess =
      context.isInternal || (context.isAdmin && context.loggedIn.aitUser.isEmpty)
    user.aitUser.map(_.id).toSeq.flatMap { userId =>
      Ait
        .getJson(
          "/api/collection?limit=100&account=" + userId,
          basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(c =>
          Some(parseCollections(c.values.toIterator.flatten, Some(user.id))))
        .getOrElse(Seq.empty)
    }
  }

  def foreignUserCollections(user: ArchUser)(implicit
      context: RequestContext = RequestContext.None): Seq[ArchCollection] = {
    foreignCollectionIds(user)
      .flatMap { aitId =>
        Ait
          .getJson("/api/collection?id=" + aitId, basicAuth = ArchConf.foreignAitAuthHeader)(c =>
            parseCollections(c.values.toIterator.flatten, Some(user.id)).headOption)
          .toOption
      }
  }
}

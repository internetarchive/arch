package org.archive.webservices.ars.model.collections

import io.circe.{HCursor, Json, JsonObject, parser}
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.io.{CollectionAccessContext, CollectionLoader, CollectionSourcePointer}
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

  private def foreignAccess(implicit context: RequestContext = RequestContext.None): Boolean = {
    context.isInternal || (context.isAdmin && context.loggedIn.aitUser.isEmpty) || context.loggedInOpt
      .exists { u =>
        AitCollectionSpecifics.foreignCollectionIds(u).contains(aitId)
      }
  }

  def inputPath: String =
    ArchConf.aitCollectionPath + s"/$aitId/" + ArchConf.aitCollectionWarcDir

  def collection(
      implicit context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    Ait
      .getJson(
        "/api/collection?id=" + aitId,
        basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(c =>
        Some(AitCollectionSpecifics.parseCollections(c.values.toIterator.flatten, userId)))
      .toOption
      .flatMap(_.headOption)
  }

  override def stats(implicit context: RequestContext): ArchCollectionStats = {
    var stats = ArchCollectionStats.Empty
    Await
      .result(
        waitAll(
          Seq(
            Future({
              Ait
                .getJson(
                  "/api/warc_file?__sum=size&limit=-1&collection=" + aitId,
                  basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(
                  _.get[Long]("size__sum").toOption)
                .getOrElse(-1L)
            }),
            Future({
              Ait
                .getJson(
                  "/api/seed?__count=id&__group=collection&deleted=false&limit=-1&collection=" + aitId,
                  basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(
                  _.downField("groups").downArray
                    .get[Int]("id__count")
                    .toOption)
                .getOrElse(-1)
            }),
            Future({
              Ait
                .getJson(
                  "/api/crawl_job_run?__group=collection&__max=processing_end_date&exclude__type__in=TEST,TEST_DELETED,TEST_EXPIRED&limit=1&collection=" + aitId,
                  basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(
                  _.downField("groups").downArray
                    .get[String]("processing_end_date__max")
                    .toOption)
                .getOrElse("")
            }))),
        30.seconds)
      .zipWithIndex
      .map {
        case (Success(v: Long), 0) => stats = stats.copy(size = v)
        case (Success(v: Int), 1) => stats = stats.copy(seeds = v)
        case (Success(v: String), 2) => stats = stats.copy(lastCrawlDate = v)
        case _ => None
      }
    stats
  }

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

  def userCollections(user: ArchUser)(
      implicit context: RequestContext = RequestContext.None): Seq[ArchCollection] = {
    val foreignAccess = context.isInternal || (context.isAdmin && context.loggedIn.aitUser.isEmpty)
    user.aitUser.map(_.id).toSeq.flatMap { userId =>
      Ait
        .getJson(
          "/api/collection?limit=100&account=" + userId,
          basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(c =>
          Some(parseCollections(c.values.toIterator.flatten, Some(user.id))))
        .getOrElse(Seq.empty)
    }
  }

  def foreignUserCollections(user: ArchUser)(
      implicit context: RequestContext = RequestContext.None): Seq[ArchCollection] = {
    foreignCollectionIds(user)
      .flatMap { aitId =>
        Ait
          .getJson("/api/collection?id=" + aitId, basicAuth = ArchConf.foreignAitAuthHeader)(c =>
            parseCollections(c.values.toIterator.flatten, Some(user.id)).headOption)
          .toOption
      }
  }
}

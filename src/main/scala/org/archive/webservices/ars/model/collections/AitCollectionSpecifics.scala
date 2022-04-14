package org.archive.webservices.ars.model.collections

import java.io.InputStream

import io.circe.{HCursor, Json, JsonObject, parser}
import javax.servlet.http.HttpServletRequest
import org.apache.spark.rdd.RDD
import org.archive.webservices.sparkling.util.StringUtil
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.io.CollectionLoader
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}

import scala.io.Source
import scala.util.Try

class AitCollectionSpecifics(id: String) extends CollectionSpecifics {
  val aitId: Int = id.stripPrefix(AitCollectionSpecifics.Prefix).toInt

  def inputPath: String =
    ArchConf.aitCollectionPath + s"/$aitId/" + ArchConf.aitCollectionWarcDir

  private def foreignAccess(implicit request: HttpServletRequest): Boolean = {
    ArchUser.get.exists { u =>
      AitCollectionSpecifics
        .foreignCollectionIds(u)
        .contains(aitId) || (u.isAdmin && u.aitUser.isEmpty)
    }
  }

  def getCollection(implicit request: HttpServletRequest): Option[ArchCollection] = {
    Ait
      .getJson(
        "/api/collection?id=" + aitId,
        basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(c =>
        Some(AitCollectionSpecifics.parseCollections(c.values.toIterator.flatten)))
      .toOption
      .flatMap(_.headOption)
  }

  def seeds(implicit request: HttpServletRequest): Int = {
    Ait
      .getJson(
        "/api/seed?__count=id&active=true&deleted=false&limit=-1&collection=" + aitId,
        basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(
        _.get[Int]("id__count").toOption)
      .getOrElse(-1)
  }

  def lastCrawlDate(implicit request: HttpServletRequest): String = {
    Ait
      .getJson(
        "/api/crawl_job_run?__group=collection&__max=processing_end_date&exclude__type__in=TEST,TEST_DELETED,TEST_EXPIRED&limit=1&collection=" + aitId,
        basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(
        _.downField("groups").downArray
          .get[String]("processing_end_date__max")
          .toOption)
      .getOrElse("")
  }

  def size(implicit request: HttpServletRequest): Long = {
    Ait
      .getJson(
        "/api/crawl_job?__group=collection&__sum=warc_content_bytes&exclude__type__in=TEST,TEST_DELETED,TEST_EXPIRED&limit=1&collection=" + aitId,
        basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(
        _.downField("groups").downArray
          .get[Long]("warc_content_bytes__sum")
          .toOption)
      .getOrElse(-1L)
  }

  def loadWarcFiles(inputPath: String): RDD[(String, InputStream)] =
    CollectionLoader.loadAitWarcFiles(aitId, inputPath, id)
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

  def parseCollections(json: Iterator[Json]): Seq[ArchCollection] = {
    json
      .map(_.hcursor)
      .flatMap { c =>
        c.get[Int]("id").right.toOption.map { aitId =>
          val collectionId = Prefix + StringUtil.padNum(aitId, 5)
          ArchCollection(
            collectionId,
            c.get[String]("name").right.getOrElse(collectionId),
            c.get[Boolean]("publicly_visible").right.getOrElse(false))
        }
      }
      .toSeq
  }

  def userCollections(user: ArchUser)(
      implicit request: HttpServletRequest): Seq[ArchCollection] = {
    val foreignAccess =
      ArchUser.get.exists(u => u.isAdmin && u.aitUser.isEmpty)
    user.aitUser.map(_.id).toSeq.flatMap { userId =>
      Ait
        .getJson(
          "/api/collection?limit=100&account=" + userId,
          basicAuth = if (foreignAccess) ArchConf.foreignAitAuthHeader else None)(c =>
          Some(parseCollections(c.values.toIterator.flatten)))
        .getOrElse(Seq.empty)
    }
  }

  def foreignUserCollections(user: ArchUser)(
      implicit request: HttpServletRequest): Seq[ArchCollection] = {
    foreignCollectionIds(user)
      .flatMap { aitId =>
        Ait
          .getJson("/api/collection?id=" + aitId, basicAuth = ArchConf.foreignAitAuthHeader)(c =>
            parseCollections(c.values.toIterator.flatten).headOption)
          .toOption
      }
  }
}

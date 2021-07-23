package org.archive.webservices.ars.model.collections

import io.circe.{Json, JsonObject, parser}
import javax.servlet.http.HttpServletRequest
import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.ait.{Ait, AitUser}
import org.archive.webservices.ars.io.CollectionLoader
import org.archive.webservices.ars.model.{ArsCloudCollection, ArsCloudConf}

import scala.io.Source
import scala.util.Try

class CohortCollectionSpecifics(id: String) extends CollectionSpecifics {
  val aitId: Int = id.stripPrefix(CohortCollectionSpecifics.Prefix).toInt

  def inputPath: String =
    ArsCloudConf.aitCollectionPath + s"/$aitId/" + ArsCloudConf.aitCollectionWarcDir

  def getCollection(implicit request: HttpServletRequest): Option[ArsCloudCollection] =
    Ait
      .user(useSession = true)
      .filter(u =>
        u.isSystemUser || CohortCollectionSpecifics.userCollectionIds(u).contains(aitId))
      .flatMap { user =>
        Ait
          .getJsonWithAuth("/api/collection?id=" + aitId, basicAuth = ArsCloudConf.aitAuthHeader)(
            c =>
              Some(AitCollectionSpecifics
                .parseCollections(c.values.toIterator.flatten, CohortCollectionSpecifics.Prefix)))
          .flatMap(_.headOption)
          .map { c =>
            c.user = Some(user)
            c
          }
      }

  def size(implicit request: HttpServletRequest): Long = {
    Ait
      .getJsonWithAuth(
        "/api/crawl_job?__group=collection&__sum=warc_content_bytes&exclude__type__in=TEST,TEST_DELETED,TEST_EXPIRED&limit=1&collection=" + aitId,
        basicAuth = ArsCloudConf.aitAuthHeader)(
        _.downField("groups").downArray
          .get[Long]("warc_content_bytes__sum")
          .toOption)
      .getOrElse(-1L)
  }

  def loadWarcs(inputPath: String): RDD[WarcRecord] =
    CollectionLoader.loadAitWarcs(aitId, inputPath, "ARCHIVEIT-" + aitId)
}

object CohortCollectionSpecifics {
  val Prefix = "COHORTS-"

  private val collectionsCursor = Try {
    val source = Source.fromFile("data/cohorts-collections.json", "utf-8")
    try {
      parser.parse(source.mkString).right.get.hcursor
    } finally {
      source.close()
    }
  }.getOrElse(Json.fromJsonObject(JsonObject.empty).hcursor)

  def userCollectionIds(user: AitUser): Seq[Int] = {
    collectionsCursor
      .downField(user.idStr)
      .values
      .toSeq
      .flatten
      .flatMap(_.asNumber.flatMap(_.toInt))
  }

  def userCollections(user: AitUser)(
      implicit request: HttpServletRequest): Seq[ArsCloudCollection] = {
    userCollectionIds(user)
      .flatMap { aitId =>
        Ait
          .getJsonWithAuth("/api/collection?id=" + aitId, basicAuth = ArsCloudConf.aitAuthHeader)(
            c =>
              AitCollectionSpecifics
                .parseCollections(c.values.toIterator.flatten, Prefix)
                .headOption)
      }
      .map { c =>
        c.user = Some(user)
        c
      }
  }
}

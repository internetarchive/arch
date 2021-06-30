package org.archive.webservices.ars.model.collections

import io.circe.Json
import javax.servlet.http.HttpServletRequest
import org.apache.spark.rdd.RDD
import org.archive.helge.sparkling.util.StringUtil
import org.archive.helge.sparkling.warc.WarcRecord
import org.archive.webservices.ars.ait.{Ait, AitUser}
import org.archive.webservices.ars.io.CollectionLoader
import org.archive.webservices.ars.model.{ArsCloudCollection, ArsCloudConf}

class AitCollectionSpecifics(id: String) extends CollectionSpecifics {
  val aitId: Int = id.stripPrefix(AitCollectionSpecifics.Prefix).toInt

  def inputPath: String =
    ArsCloudConf.aitCollectionPath + s"/$aitId/" + ArsCloudConf.aitCollectionWarcDir

  def getCollection(implicit request: HttpServletRequest): Option[ArsCloudCollection] = {
    Ait
      .getJson("/api/collection?id=" + aitId)(c =>
        Some(AitCollectionSpecifics.parseCollections(c.values.toIterator.flatten)))
      .flatMap(_.headOption)
      .map { c =>
        c.user = Ait.user(useSession = true)
        c
      }
  }

  def size(implicit request: HttpServletRequest): Long = {
    Ait
      .getJson(
        "/api/crawl_job?__group=collection&__sum=warc_content_bytes&exclude__type__in=TEST,TEST_DELETED,TEST_EXPIRED&limit=1&collection=" + aitId)(
        _.downField("groups").downArray
          .get[Long]("warc_content_bytes__sum")
          .toOption)
      .getOrElse(-1L)
  }

  def loadWarcs(inputPath: String): RDD[WarcRecord] =
    CollectionLoader.loadAitWarcs(aitId, inputPath, id)
}

object AitCollectionSpecifics {
  val Prefix = "ARCHIVEIT-"
  val WasapiPageSize = 10

  def parseCollections(json: Iterator[Json], prefix: String = Prefix): Seq[ArsCloudCollection] = {
    json
      .map(_.hcursor)
      .flatMap { c =>
        c.get[Int]("id").right.toOption.map { aitId =>
          val collectionId = prefix + StringUtil.padNum(aitId, 5)
          ArsCloudCollection(
            collectionId,
            c.get[String]("name").right.getOrElse(collectionId),
            c.get[Boolean]("publicly_visible").right.getOrElse(false))
        }
      }
      .toSeq
  }

  def userCollections(user: AitUser)(
      implicit request: HttpServletRequest): Seq[ArsCloudCollection] = {
    Ait
      .getJson("/api/collection?limit=100&account=" + user.id)(c =>
        Some(parseCollections(c.values.toIterator.flatten)))
      .getOrElse(Seq.empty)
      .map { c =>
        c.user = Some(user)
        c
      }
  }
}

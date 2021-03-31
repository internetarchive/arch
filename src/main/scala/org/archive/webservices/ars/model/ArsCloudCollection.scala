package org.archive.webservices.ars.model

import io.circe.HCursor
import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.ait.{Ait, AitUser}
import org.scalatra.guavaCache.GuavaCache

case class ArsCloudCollection(id: String, name: String, public: Boolean) {
  def info: ArsCloudCollectionInfo = ArsCloudCollectionInfo.get(id)
  var user: Option[AitUser] = None
}

object ArsCloudCollection {
  val AitPrefix = "ARCHIVEIT-"

  def inputPath(id: String): Option[String] = {
    if (id.startsWith(AitPrefix)) {
      val aitId = id.stripPrefix(AitPrefix)
      Some(
        ArsCloudConf.aitCollectionPath + s"/$aitId/" + ArsCloudConf.aitCollectionWarcDir + "/*.warc.gz")
    } else None
  }

  private def cacheKey(id: String): String = getClass.getSimpleName + id

  def get(id: String)(implicit request: HttpServletRequest): Option[ArsCloudCollection] = {
    val key = cacheKey(id)
    GuavaCache
      .get[ArsCloudCollection](key)
      .filter(c => c.user.isEmpty || Ait.user.exists(_.id == c.user.get.id))
      .orElse {
        if (id.startsWith(AitPrefix)) {
          val aitId = id.stripPrefix(AitPrefix)
          Ait.getJson("/api/collection?id=" + aitId)(parseJson).flatMap(_.headOption).map {
            collection =>
              collection.user = Ait.user(useSession = true)
              GuavaCache.put(key, collection, None)
          }
        } else None
      }
  }

  private def parseJson(cursor: HCursor): Option[Seq[ArsCloudCollection]] = {
    cursor.values.map(_.map(_.hcursor).flatMap { c =>
      c.get[Int]("id").right.toOption.map { aitId =>
        val collectionId = AitPrefix + aitId
        GuavaCache.put(
          cacheKey(collectionId), {
            ArsCloudCollection(
              collectionId,
              c.get[String]("name").right.getOrElse(collectionId),
              c.get[Boolean]("publicly_visible").right.getOrElse(false))
          },
          None)
      }
    }.toSeq)
  }

  def userCollections(user: AitUser)(
      implicit request: HttpServletRequest): Seq[ArsCloudCollection] = {
    Ait.getJson("/api/collection?limit=100&account=" + user.id)(parseJson).getOrElse(Seq.empty)
  }
}

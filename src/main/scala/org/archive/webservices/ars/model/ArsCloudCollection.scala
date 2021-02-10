package org.archive.webservices.ars.model

import io.circe.HCursor
import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.ait.{Ait, AitUser}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.scalatra.guavaCache.GuavaCache

case class ArsCloudCollection(id: String, name: String) {
  def jobConfig: Option[DerivationJobConf] = DerivationJobConf.collection(id)
}

object ArsCloudCollection {
  val AitPrefix = "ARCHIVEIT-"

  private def cacheKey(id: String): String = getClass.getSimpleName + id

  def get(id: String)(implicit request: HttpServletRequest): Option[ArsCloudCollection] = {
    GuavaCache.get[ArsCloudCollection](cacheKey(id)).orElse {
      if (id.startsWith(AitPrefix)) {
        Ait.getJson("/api/collection?id=" + id)(parseJson).flatMap(_.headOption)
      } else None
    }
  }

  private def parseJson(cursor: HCursor): Option[Seq[ArsCloudCollection]] = {
    cursor.values.map(_.map(_.hcursor).flatMap { c =>
      c.get[Int]("id").right.toOption.map { aitId =>
        val collectionId = AitPrefix + aitId
        GuavaCache.put(cacheKey(collectionId), {
          ArsCloudCollection(collectionId, c.get[String]("name").right.getOrElse(collectionId))
        }, None)
      }
    }.toSeq)
  }

  def userCollections(user: AitUser)(
      implicit request: HttpServletRequest): Seq[ArsCloudCollection] = {
    Ait.getJson("/api/collection?limit=100&account=" + user.id)(parseJson).getOrElse(Seq.empty)
  }
}

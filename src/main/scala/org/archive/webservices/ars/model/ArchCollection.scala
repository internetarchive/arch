package org.archive.webservices.ars.model

import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.model.collections.{
  AitCollectionSpecifics,
  CollectionSpecifics,
  SpecialCollectionSpecifics
}
import org.archive.webservices.ars.model.users.ArchUser
import org.scalatra.guavaCache.GuavaCache

case class ArchCollection(id: String, name: String, public: Boolean) {
  private var user: Option[ArchUser] = None

  def info: ArchCollectionInfo = ArchCollectionInfo.get(id)

  private var statsLoaded = false
  def ensureStats()(implicit request: HttpServletRequest): Unit = {
    if (!statsLoaded) {
      statsLoaded = true
      for (c <- CollectionSpecifics.get(id)) _size = c.size
    }
  }

  private var _size: Long = -1
  def size: Long = _size
}

object ArchCollection {
  def inputPath(id: String): Option[String] = CollectionSpecifics.get(id).map(_.inputPath)

  private def cacheKey(id: String): String = getClass.getSimpleName + id

  def get(id: String)(implicit request: HttpServletRequest): Option[ArchCollection] = {
    val user = ArchUser.get
    (if (ArchConf.production) GuavaCache.get[ArchCollection](cacheKey(id)) else None)
      .filter { c =>
        user
          .exists(u => u.isAdmin || c.user.map(_.id).contains(u.id))
      }
      .orElse {
        CollectionSpecifics.get(id).flatMap(_.getCollection).map { c =>
          if (ArchConf.production) {
            c.user = user
            GuavaCache.put(cacheKey(c.id), c, None)
          } else c
        }
      }
  }

  def userCollections(user: ArchUser)(
      implicit request: HttpServletRequest): Seq[ArchCollection] = {
    (AitCollectionSpecifics.userCollections(user) ++ AitCollectionSpecifics
      .foreignUserCollections(user) ++ SpecialCollectionSpecifics.userCollections(user))
      .map { c =>
        if (ArchConf.production) {
          c.user = Some(user)
          GuavaCache.put(cacheKey(c.id), c, None)
        } else c
      }
      .sortBy(_.name.toLowerCase)
  }
}

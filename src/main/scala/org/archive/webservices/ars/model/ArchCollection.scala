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
      for (c <- CollectionSpecifics.get(id)) {
        _size = c.size
        _seeds = c.seeds
        _lastCrawlDate = c.lastCrawlDate
      }
    }
  }

  private var _size: Long = -1
  def size: Long = _size

  private var _seeds: Int = -1
  def seeds: Int = _seeds

  private var _lastCrawlDate: String = ""
  def lastCrawlDate: String = _lastCrawlDate
}

object ArchCollection {
  def inputPath(id: String): Option[String] = CollectionSpecifics.get(id).map(_.inputPath)

  private def cacheKey(id: String): String = getClass.getSimpleName + id

  def get(id: String)(implicit request: HttpServletRequest): Option[ArchCollection] =
    getInternal(id, Some(request))

  def getInternal(
      id: String,
      request: Option[HttpServletRequest] = None): Option[ArchCollection] = {
    (if (ArchConf.production) GuavaCache.get[ArchCollection](cacheKey(id)) else None)
      .filter { c =>
        request.isEmpty || request
          .flatMap(ArchUser.get(_))
          .exists(u => u.isAdmin || c.user.map(_.id).contains(u.id))
      }
      .orElse {
        CollectionSpecifics
          .get(id)
          .flatMap { specifics =>
            specifics.getCollection(request)
          }
          .map { c =>
            if (ArchConf.production) {
              if (request.isDefined) c.user = request.flatMap(ArchUser.get(_))
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

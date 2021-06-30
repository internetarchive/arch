package org.archive.webservices.ars.model

import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.ait.{Ait, AitUser}
import org.archive.webservices.ars.model.collections.{
  AitCollectionSpecifics,
  CohortCollectionSpecifics,
  CollectionSpecifics
}
import org.scalatra.guavaCache.GuavaCache

case class ArsCloudCollection(id: String, name: String, public: Boolean) {
  def info: ArsCloudCollectionInfo = ArsCloudCollectionInfo.get(id)
  var user: Option[AitUser] = None

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

object ArsCloudCollection {
  def inputPath(id: String): Option[String] = CollectionSpecifics.get(id).map(_.inputPath)

  private def cacheKey(id: String): String = getClass.getSimpleName + id

  def get(id: String)(implicit request: HttpServletRequest): Option[ArsCloudCollection] = {
    (if (ArsCloudConf.production) GuavaCache.get[ArsCloudCollection](cacheKey(id)) else None)
      .filter(c => c.user.isEmpty || Ait.user.exists(_.id == c.user.get.id))
      .orElse {
        val c = CollectionSpecifics.get(id).flatMap(_.getCollection)
        if (ArsCloudConf.production && c.isDefined) GuavaCache.put(cacheKey(id), c.get, None)
        c
      }
  }

  def userCollections(user: AitUser)(
      implicit request: HttpServletRequest): Seq[ArsCloudCollection] = {
    (AitCollectionSpecifics.userCollections(user) ++ CohortCollectionSpecifics.userCollections(
      user))
      .map { c =>
        if (ArsCloudConf.production) GuavaCache.put(cacheKey(c.id), c, None)
        else c
      }
      .sortBy(_.name.toLowerCase)
  }
}

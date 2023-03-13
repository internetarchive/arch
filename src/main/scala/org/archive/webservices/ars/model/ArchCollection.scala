package org.archive.webservices.ars.model

import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.{AitCollectionSpecifics, CollectionSpecifics, CustomCollectionSpecifics, SpecialCollectionSpecifics}
import org.archive.webservices.ars.model.users.ArchUser
import org.scalatra.guavaCache.GuavaCache

import javax.servlet.http.HttpServletRequest

case class ArchCollection(
    id: String,
    name: String,
    public: Boolean,
    userSpecificId: Option[String] = None) {
  private var user: Option[ArchUser] = None

  def userUrlId: String = userSpecificId.getOrElse(id)

  private var statsLoaded = false
  def ensureStats()(implicit context: RequestContext): Unit = {
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
  val UserIdSeparator = ":"
  val PathUserEscape = "-"

  private def cacheKey(id: String): String = getClass.getSimpleName + id

  def get(id: String)(
      implicit context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    (if (ArchConf.production) GuavaCache.get[ArchCollection](cacheKey(id)) else None)
      .filter { c =>
        context.isInternal || context.loggedIn.isAdmin || c.user
          .map(_.id)
          .contains(context.loggedIn.id)
      }
      .orElse {
        CollectionSpecifics
          .get(id, context.user)
          .flatMap(_.collection)
          .map { c =>
            if (ArchConf.production) {
              for (u <- context.loggedInOpt) c.user = Some(u)
              GuavaCache.put(cacheKey(c.id), c, None)
            } else c
          }
      }
  }

  def userCollections(user: ArchUser)(
      implicit context: RequestContext = RequestContext.None): Seq[ArchCollection] = {
    (AitCollectionSpecifics.userCollections(user) ++ AitCollectionSpecifics
      .foreignUserCollections(user) ++ SpecialCollectionSpecifics.userCollections(user) ++ CustomCollectionSpecifics
      .userCollections(user))
      .map { c =>
        if (ArchConf.production) {
          c.user = Some(user)
          GuavaCache.put(cacheKey(c.id), c, None)
        } else c
      }
      .sortBy(_.name.toLowerCase)
  }

  def splitIdUserCollection(idWithoutPrefix: String): Option[(String, String)] = {
    val split = idWithoutPrefix.split(UserIdSeparator)
    if (split.length > 2) Some {
      val collection = split.last
      val user = split.dropRight(1).mkString(UserIdSeparator)
      (user, collection)
    } else None
  }

  def id(id: String, prefix: String, user: ArchUser): String = {
    val (p, c) = if (id.startsWith(prefix)) (prefix, id.stripPrefix(prefix)) else ("", id)
    p + (splitIdUserCollection(c) match {
      case Some(_) => c
      case None => user.id + ArchCollection.UserIdSeparator + c
    })
  }

  def id(id: String, user: ArchUser): String = {
    prefix(id).map { p =>
      val c = id.stripPrefix(p)
      p + (splitIdUserCollection(c) match {
        case Some(_) => c
        case None => user.id + ArchCollection.UserIdSeparator + c
      })
    }.getOrElse(id)
  }

  def id(id: String)(
    implicit context: RequestContext = RequestContext.None): String = {
    prefix(id).map { p =>
      val c = id.stripPrefix(p)
      p + (splitIdUserCollection(c) match {
        case Some(_) => c
        case None => context.userOpt.map(_.id + ArchCollection.UserIdSeparator + c).getOrElse(c)
      })
    }.getOrElse(id)
  }

  def prefix(id: String): Option[String] = {
    if (id.startsWith(AitCollectionSpecifics.Prefix)) {
      Some(AitCollectionSpecifics.Prefix)
    } else if (id.startsWith(SpecialCollectionSpecifics.Prefix)) {
      Some(SpecialCollectionSpecifics.Prefix)
    } else if (id.startsWith(CustomCollectionSpecifics.Prefix)) {
      Some(CustomCollectionSpecifics.Prefix)
    } else None
  }
}

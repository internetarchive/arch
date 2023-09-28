package org.archive.webservices.ars.model

import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections._
import org.archive.webservices.ars.model.users.ArchUser
import org.scalatra.guavaCache.GuavaCache

case class ArchCollection(
    id: String,
    name: String,
    public: Boolean,
    userSpecificId: Option[(String, String)] = None,
    sourceId: String) {
  private var user: Option[ArchUser] = None

  def userUrlId(implicit context: RequestContext): String = userUrlId(context.user.id)
  def userUrlId(userId: String): String =
    userSpecificId.filter(_._1 == userId).map(_._2).getOrElse(id)

  lazy val specifics: CollectionSpecifics = CollectionSpecifics.get(id) match {
    case Some(specifics) => specifics
    case None => throw new RuntimeException("No specifics found for collection " + id)
  }

  private var _stats: Option[ArchCollectionStats] = None
  def stats(implicit context: RequestContext = RequestContext.None): ArchCollectionStats = {
    _stats.orElse {
      synchronized {
        _stats = Some(specifics.stats)
        _stats
      }
    }.get
  }
}

object ArchCollection {
  val UserIdSeparator = ":"
  val PathUserEscape = "-"

  private def cacheKey(id: String): String = getClass.getSimpleName + id

  def get(id: String)(implicit
      context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    (if (!ArchConf.isDev) GuavaCache.get[ArchCollection](cacheKey(id)) else None)
      .filter { c =>
        context.isInternal || context.loggedIn.isAdmin || c.user
          .map(_.id)
          .contains(context.loggedIn.id)
      }
      .orElse {
        CollectionSpecifics
          .get(id)
          .flatMap(_.collection)
          .map { c =>
            if (!ArchConf.isDev) {
              for (u <- context.loggedInOpt) c.user = Some(u)
              GuavaCache.put(cacheKey(c.id), c, None)
            } else c
          }
      }
  }

  def userCollections(user: ArchUser)(implicit
      context: RequestContext = RequestContext.None): Seq[ArchCollection] = {
    (AitCollectionSpecifics.userCollections(user) ++ AitCollectionSpecifics
      .foreignUserCollections(user) ++ SpecialCollectionSpecifics.userCollections(
      user) ++ CustomCollectionSpecifics
      .userCollections(user))
      .map(c =>
        if (!ArchConf.isDev) {
          GuavaCache
            .get[ArchCollection](cacheKey(c.id))
            .getOrElse({
              c.user = Some(user)
              GuavaCache.put(cacheKey(c.id), c, None)
              c
            })
        } else c)
      .sortBy(_.name.toLowerCase)
  }

  def splitIdUserCollection(idWithoutPrefix: String): (Option[String], String) = {
    val split = idWithoutPrefix.split(UserIdSeparator)
    if (split.length > 2) {
      val collection = split.last
      val user = split.dropRight(1).mkString(UserIdSeparator)
      (Some(user), collection)
    } else (None, idWithoutPrefix)
  }

  def splitIdUserCollectionOpt(idWithoutPrefix: String): Option[(String, String)] = {
    val (userOpt, collection) = splitIdUserCollection(idWithoutPrefix)
    userOpt.map((_, collection))
  }

  def splitIdUserCollectionOpt(id: String, prefix: String): Option[(String, String)] = {
    val (userOpt, collection) = splitIdUserCollection(id.stripPrefix(prefix))
    userOpt.map((_, prefix + collection))
  }

  def prependUserId(idWithoutPrefix: String, userId: String): String = {
    userId + ArchCollection.UserIdSeparator + idWithoutPrefix
  }

  def prependUserId(idWithoutPrefix: String, userId: String, prefix: String): String = {
    prefix + prependUserId(idWithoutPrefix, userId)
  }

  def prependUserId(idWithoutPrefix: String, userId: Option[String]): String = {
    userId.map(prependUserId(idWithoutPrefix, _)).getOrElse(idWithoutPrefix)
  }

  def prependUserId(id: String, userId: Option[String], prefix: String): String = {
    prefix + prependUserId(id.stripPrefix(prefix), userId)
  }

  def userCollectionId(id: String, prefix: String, user: ArchUser): String = {
    val (p, c) = if (id.startsWith(prefix)) (prefix, id.stripPrefix(prefix)) else ("", id)
    p + (splitIdUserCollectionOpt(c) match {
      case Some(_) => c
      case None => prependUserId(c, Some(user.id))
    })
  }

  def userCollectionId(id: String, user: ArchUser): String = {
    prefix(id)
      .map { p =>
        val c = id.stripPrefix(p)
        p + (splitIdUserCollectionOpt(c) match {
          case Some(_) => c
          case None => prependUserId(c, Some(user.id))
        })
      }
      .getOrElse(id)
  }

  def userCollectionId(id: String)(implicit context: RequestContext): String = {
    prefix(id)
      .map { p =>
        val c = id.stripPrefix(p)
        p + (splitIdUserCollectionOpt(c) match {
          case Some(_) => c
          case None => prependUserId(c, context.userOpt.map(_.id))
        })
      }
      .getOrElse(id)
  }

  def prefix(id: String): Option[String] = {
    if (id.startsWith(AitCollectionSpecifics.Prefix)) {
      Some(AitCollectionSpecifics.Prefix)
    } else if (id.startsWith(SpecialCollectionSpecifics.Prefix)) {
      Some(SpecialCollectionSpecifics.Prefix)
    } else if (id.startsWith(CustomCollectionSpecifics.Prefix)) {
      Some(CustomCollectionSpecifics.Prefix)
    } else if (id.startsWith(UnionCollectionSpecifics.Prefix)) {
      Some(UnionCollectionSpecifics.Prefix)
    } else None
  }
}

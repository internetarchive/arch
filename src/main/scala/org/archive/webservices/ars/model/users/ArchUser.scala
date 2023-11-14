package org.archive.webservices.ars.model.users

import io.circe.{HCursor, Json, JsonObject, parser}
import io.github.nremond.SecureHash
import org.archive.webservices.ars.ait.{Ait, AitUser}
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.util.DatafileUtil
import org.archive.webservices.sparkling.util.{DigestUtil, StringUtil}
import org.scalatra.servlet.ServletApiImplicits._

import java.util.Base64
import java.util.UUID.randomUUID
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.io.Source
import scala.util.Try

trait ArchUser {
  def id: String
  def userName: String
  def fullName: String
  def email: Option[String]
  def isAdmin: Boolean
  def isUser: Boolean
  def aitUser: Option[AitUser] = None
  lazy val urlId: String = aitUser.map(_.id.toString).getOrElse(id)
  lazy val datafileKey: String = id.stripPrefix("arch:")
  def option: Option[ArchUser] = if (isUser) Some(this) else None
}

object ArchUser {
  val None = DefaultArchUser("", "", "", scala.None, isAdmin = false, isUser = false)
  val PrefixNameSeparator = ":"

  private var _archUsersCursor: Option[HCursor] = scala.None
  private var _archAdminApiKeys: Set[String] = Set.empty
  private def archUsersCursor: HCursor = _archUsersCursor.getOrElse {
    val cur = DatafileUtil.load("arch-users.json").hcursor
    _archUsersCursor = Some(cur)
    // Collect admin API keys.
    _archAdminApiKeys = cur.keys
      .getOrElse(Seq.empty)
      .map { cur.downField(_) }
      .filter { _.get[Boolean]("admin").getOrElse(false) }
      .flatMap { _.get[String]("apiKey").toOption }
      .toSet
    cur
  }

  private def isAdminApiKey(apiKey: String): Boolean =
    _archAdminApiKeys.exists(SecureHash.validatePassword(apiKey, _))

  private def isUserApiKey(username: String, apiKey: String): Boolean =
    archUsersCursor
      .downField(username)
      .get[String]("apiKey")
      .map(SecureHash.validatePassword(apiKey, _))
      .getOrElse(false)

  private var _aitUserIds: Option[Set[Int]] = scala.None
  private def aitUserIds: Set[Int] = _aitUserIds.getOrElse {
    _aitUserIds = Some(
      DatafileUtil
        .load("ait-users.json")
        .hcursor
        .downField("ids")
        .values
        .getOrElse(Iterator.empty)
        .flatMap(_.asNumber.flatMap(_.toInt))
        .toSet)
    _aitUserIds.get
  }

  def invalidateData(): Unit = {
    _archUsersCursor = scala.None
    _aitUserIds = scala.None
  }

  def create(name: String, admin: Boolean = false): String = {
    val json = DatafileUtil.loadArchUsers
    if (json.hcursor.keys.get.toSet.contains(name)) {
      throw new Error(s"A user already exists with the name: $name")
    }
    // Generate an API key.
    val apiKey = randomUUID.toString
    DatafileUtil.storeArchUsers(
      json.deepMerge(
        Map(
          name -> Map(
            "name" -> name.asJson,
            "admin" -> admin.asJson,
            "apiKey" -> SecureHash.createHash(apiKey).toString.asJson)).asJson))
    apiKey
  }

  def rollApiKey(name: String): String = {
    val json = DatafileUtil.loadArchUsers
    if (!json.hcursor.keys.get.toSet.contains(name)) {
      throw new Error(s"No user exists with the name: $name")
    }
    // Generate a new API key.
    val apiKey = randomUUID.toString
    DatafileUtil.storeArchUsers(
      json.hcursor
        .downField(name)
        .downField("apiKey")
        .withFocus(_ => SecureHash.createHash(apiKey).toString.asJson)
        .top
        .get)
    apiKey
  }

  def get(id: String, apiKey: Option[String] = scala.None): Option[ArchUser] = {
    val (usersKey, finalId, username) =
      if (!id.contains(":")) (id, s"arch:$id", id)
      else
        (
          id.split(":", 2) match {
            case Array("arch", name) => (name, id, name)
            case Array(_, name) => (id, id, name)
          }
        )

    Some(archUsersCursor)
      .map(_.downField(usersKey))
      .filter(u =>
        apiKey.isEmpty
          || isUserApiKey(usersKey, apiKey.get)
          || isAdminApiKey(apiKey.get))
      .map(cursor =>
        DefaultArchUser(
          finalId,
          username,
          cursor.get[String]("name").getOrElse(username),
          cursor.get[String]("email").toOption.map(_.trim).filter { email =>
            !email.contains(" ") && {
              val split = email.split("@")
              split.length == 2 && split(0).nonEmpty && {
                val domainSplit = split(1).split('.')
                domainSplit.length > 1 && domainSplit.forall(_.nonEmpty)
              }
            }
          },
          cursor.get[Boolean]("admin").getOrElse(false)))
  }

  def get(id: String): Option[ArchUser] = get(id = id, apiKey = scala.None)
}

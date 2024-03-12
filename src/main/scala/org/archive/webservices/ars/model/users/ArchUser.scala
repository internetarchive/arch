package org.archive.webservices.ars.model.users

import io.circe.{HCursor, Json, JsonObject, parser}
import org.archive.webservices.ars.ait.{Ait, AitUser}
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.sparkling.util.{DigestUtil, StringUtil}
import org.scalatra.servlet.ServletApiImplicits._

import java.util.Base64
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
  def option: Option[ArchUser] = if (isUser) Some(this) else None
}

object ArchUser {
  val AitPrefix = "ait"
  val ArchPrefix = "arch"
  val PrefixNameSeparator = ":"

  val UserSessionAttribute = "arch-user"

  val None = DefaultArchUser("", "", "", scala.None, isAdmin = false, isUser = false)

  private var _archUsersCursor: Option[HCursor] = scala.None
  private def archUsersCursor: HCursor = _archUsersCursor.getOrElse {
    _archUsersCursor = Some(Try {
      val source = Source.fromFile("data/arch-users.json", "utf-8")
      try {
        parser.parse(source.mkString).right.get.hcursor
      } finally {
        source.close()
      }
    }.getOrElse(Json.fromJsonObject(JsonObject.empty).hcursor))
    _archUsersCursor.get
  }

  private var _aitUserIds: Option[Set[Int]] = scala.None
  private def aitUserIds: Set[Int] = _aitUserIds.getOrElse {
    _aitUserIds = Some(Try {
      val source = Source.fromFile("data/ait-users.json", "utf-8")
      try {
        parser
          .parse(source.mkString)
          .right
          .get
          .hcursor
          .downField("ids")
          .values
          .getOrElse(Iterator.empty)
          .flatMap(_.asNumber.flatMap(_.toInt))
          .toSet
      } finally {
        source.close()
      }
    }.getOrElse(Set.empty))
    _aitUserIds.get
  }

  def invalidateData(): Unit = {
    _archUsersCursor = scala.None
    _aitUserIds = scala.None
  }

  private def archUser(name: String, password: Option[String] = scala.None): Option[ArchUser] =
    Some(archUsersCursor)
      .map(_.downField(name.stripPrefix(ArchPrefix + "_")))
      .filter(password.isEmpty || _.get[String]("password")
        .contains("sha1:" + DigestUtil.sha1Base32(password.get)))
      .map { cursor =>
        DefaultArchUser(
          ArchPrefix + PrefixNameSeparator + name,
          name,
          cursor.get[String]("name").getOrElse(name),
          cursor.get[String]("email").toOption.map(_.trim).filter { email =>
            !email.contains(" ") && {
              val split = email.split("@")
              split.length == 2 && split(0).nonEmpty && {
                val domainSplit = split(1).split('.')
                domainSplit.length > 1 && domainSplit.forall(_.nonEmpty)
              }
            }
          },
          cursor.get[Boolean]("admin").getOrElse(false))
      }

  def login(username: String, password: String)(implicit
      request: HttpServletRequest,
      response: HttpServletResponse): Option[String] = {
    val (prefix, name) =
      if (username.contains(PrefixNameSeparator))
        (
          StringUtil.prefixBySeparator(username, PrefixNameSeparator),
          StringUtil.stripPrefixBySeparator(username, PrefixNameSeparator))
      else (ArchPrefix, username)
    (if (ArchConf.forceKeystoneLogin) KeystoneUser.prefix else prefix) match {
      case ArchPrefix =>
        archUser(name, Some(password)) match {
          case Some(user) =>
            request.getSession.setAttribute(UserSessionAttribute, user)
            scala.None
          case scala.None =>
            Some("Wrong username or password")
        }
      case AitPrefix =>
        Ait.login(name, password, response).left.toOption
      case KeystoneUser.prefix =>
        KeystoneUser.login(name, password) match {
          case Some(user) => {
            request.getSession.setAttribute(UserSessionAttribute, user)
            scala.None
          }
          case scala.None => Some("Wrong username or password")
        }
      case _ =>
        Some("User not found.")
    }
  }

  def logout()(implicit request: HttpServletRequest): Unit = {
    get match {
      case Some(u) =>
        if (u.aitUser.isDefined) Ait.logout()
        else request.getSession.removeAttribute(UserSessionAttribute)
      case scala.None => // do nothing
    }
  }

  def get(implicit request: HttpServletRequest): Option[ArchUser] = get(useSession = true)

  def get(useSession: Boolean)(implicit request: HttpServletRequest): Option[ArchUser] = {
    Option(request.getSession.getAttribute(UserSessionAttribute))
      .map(_.asInstanceOf[ArchUser])
      .orElse {
        request
          .header("Authorization")
          .filter(_ != null)
          .map(_.trim)
          .filter(_.toLowerCase.startsWith("basic "))
          .map(StringUtil.stripPrefixBySeparator(_, " "))
          .flatMap { base64 =>
            val userPassword = new String(Base64.getDecoder.decode(base64), "utf-8")
            val Array(user, password) = userPassword.stripPrefix(ArchPrefix + PrefixNameSeparator).split(PrefixNameSeparator, 2)
            archUser(user, Some(password))
          }
      }
  }

  def get(id: String)(implicit
      context: RequestContext = RequestContext.None): Option[ArchUser] = {
    val (prefix, suffix) =
      if (id.contains(PrefixNameSeparator))
        (StringUtil.prefixBySeparator(id, PrefixNameSeparator), StringUtil.stripPrefixBySeparator(id, PrefixNameSeparator))
      else (AitPrefix, id)
    prefix match {
      case ArchPrefix =>
        archUser(suffix)
      case AitPrefix =>
        Try(suffix.toInt).toOption.flatMap(Ait.user(_)).map(AitArchUser(_))
      case KeystoneUser.prefix => KeystoneUser.get(suffix)
      case _ =>
        scala.None
    }
  }
}

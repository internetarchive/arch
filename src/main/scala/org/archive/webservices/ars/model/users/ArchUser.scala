package org.archive.webservices.ars.model.users

import io.circe.parser
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.archive.helge.sparkling.util.{DigestUtil, StringUtil}
import org.archive.webservices.ars.ait.{Ait, AitUser}

import scala.io.Source
import scala.util.Try

trait ArchUser {
  def id: String
  def userName: String
  def fullName: String
  def email: Option[String]
  def isAdmin: Boolean
  def isLoggedIn: Boolean
  def aitUser: Option[AitUser] = None
  lazy val urlId: String = aitUser.map(_.id.toString).getOrElse(id)
}

object ArchUser {
  val AitPrefix = "ait"
  val ArchPrefix = "arch"

  val UserSessionAttribute = "arch-user"

  val NoUser = DefaultArchUser("", "", "", None, isAdmin = false, isLoggedIn = false)

  private def archUser(name: String, password: Option[String] = None): Option[ArchUser] =
    Try {
      val source = Source.fromFile("data/arch-users.json", "utf-8")
      try {
        parser.parse(source.mkString).right.get.hcursor
      } finally {
        source.close()
      }
    }.toOption
      .map(_.downField(name))
      .filter(password.isEmpty || _.get[String]("password")
        .contains("sha1:" + DigestUtil.sha1Base32(password.get)))
      .map { cursor =>
        DefaultArchUser(
          ArchPrefix + ":" + name,
          name,
          cursor.get[String]("name").getOrElse(name),
          cursor.get[String]("email").toOption.map(_.trim).filter { email =>
            !email.contains(" ") && {
              val split = email.split("@")
              split.length == 2 && split(0).nonEmpty && {
                val domainSplit = split(1).split(".")
                domainSplit.length > 1 && domainSplit.forall(_.nonEmpty)
              }
            }
          },
          cursor.get[Boolean]("admin").getOrElse(false))
      }

  def login(username: String, password: String)(
      implicit request: HttpServletRequest,
      response: HttpServletResponse): Option[String] = {
    val (prefix, name) =
      if (username.contains(":"))
        (
          StringUtil.prefixBySeparator(username, ":"),
          StringUtil.stripPrefixBySeparator(username, ":"))
      else (ArchPrefix, username)
    prefix match {
      case ArchPrefix =>
        archUser(name, Some(password)) match {
          case Some(user) =>
            request.getSession.setAttribute(UserSessionAttribute, user)
            None
          case None =>
            Some("Wrong username or password!")
        }
      case AitPrefix =>
        Ait.login(name, password, response).left.toOption
      case _ =>
        Some("User not found.")
    }
  }

  def logout()(implicit request: HttpServletRequest): Unit = {
    get match {
      case Some(u) =>
        if (u.aitUser.isDefined) Ait.logout()
        else request.getSession.removeAttribute(UserSessionAttribute)
      case None => // do nothing
    }
  }

  def get(implicit request: HttpServletRequest): Option[ArchUser] = get(useSession = true)

  def get(useSession: Boolean)(implicit request: HttpServletRequest): Option[ArchUser] = {
    Option(request.getSession.getAttribute(UserSessionAttribute))
      .map(_.asInstanceOf[ArchUser])
      .orElse {
        Ait.user(useSession).map(AitArchUser(_))
      }
  }

  def get(id: String)(implicit request: HttpServletRequest): Option[ArchUser] = {
    val (prefix, suffix) =
      if (id.contains(":"))
        (StringUtil.prefixBySeparator(id, ":"), StringUtil.stripPrefixBySeparator(id, ":"))
      else (AitPrefix, id)
    prefix match {
      case ArchPrefix =>
        archUser(suffix)
      case AitPrefix =>
        Try(suffix.toInt).toOption.flatMap(Ait.user(_)).map(AitArchUser(_))
      case _ =>
        None
    }
  }
}

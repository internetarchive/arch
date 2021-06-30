package org.archive.webservices.ars.ait

import java.io.{InputStream, PrintWriter}
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.util.Base64

import io.circe.HCursor
import io.circe.parser._
import javax.net.ssl.HttpsURLConnection
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.archive.helge.sparkling.util.StringUtil
import org.scalatra.Cookie
import org.scalatra.servlet.ServletApiImplicits._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Try

object Ait {
  val AitSessionRequestAttribute = "AIT-sessionid"
  val AitSessionCookie = "sessionid"
  val UserSessionAttribute = "ait-user"
  val SystemUserId = 188

  def user(implicit request: HttpServletRequest): Option[AitUser] = user(useSession = false)

  def user(useSession: Boolean)(implicit request: HttpServletRequest): Option[AitUser] = {
    (if (useSession)
       Option(request.getSession.getAttribute(UserSessionAttribute)).map(_.asInstanceOf[AitUser])
     else None).orElse {
      getJson("/api/auth/list") { json =>
        for {
          userName <- json.get[String]("username").toOption
          id <- json
            .downField("account")
            .get[Int]("id")
            .toOption
            .map(id => if (id == SystemUserId) 0 else id)
        } yield {
          val user =
            AitUser(id, userName, json.get[String]("full_name").toOption.getOrElse(userName))
          request.getSession.setAttribute(UserSessionAttribute, user)
          user
        }
      }
    }
  }

  def user(id: Int)(implicit request: HttpServletRequest): Option[AitUser] =
    getJson("/api/user?limit=1&account=" + (if (id == 0) SystemUserId else id)) { json =>
      val user = json.downArray
      for {
        userName <- user.get[String]("username").toOption
      } yield AitUser(id, userName, user.get[String]("full_name").toOption.getOrElse(userName))
    }

  def login(username: String, password: String, response: HttpServletResponse)(
      implicit request: HttpServletRequest): Either[String, String] = {
    val either = login(username, password)
    for (sessionid <- either.right.toOption)
      response.addCookie(Cookie(AitSessionCookie, sessionid))
    either
  }

  def login(username: String, password: String)(
      implicit request: HttpServletRequest): Either[String, String] = {
    val ait = new URL("https://partner.archive-it.org/login").openConnection
      .asInstanceOf[HttpsURLConnection]
    try {
      ait.setRequestMethod("POST")
      ait.setDoInput(true)
      ait.setDoOutput(true)
      ait.setInstanceFollowRedirects(false)
      ait.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      val out = ait.getOutputStream
      try {
        val postBody = Seq(("username", username.toLowerCase), ("password", password))
          .map {
            case (k, v) =>
              URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
          }
          .mkString("&") + "&next="
        val writer = new PrintWriter(out)
        writer.write(postBody)
        writer.flush()
        writer.close()
      } finally {
        Try(out.close())
      }
      ait.connect()
      val location = ait.getHeaderField("Location")
      val isSystemAccount = location != null && location.endsWith("/choose_account")
      if (ait.getResponseCode == 302 && location != null && (isSystemAccount || location.matches(
            "^\\/\\d+$"))) {
        ait.getHeaderFields
          .get("set-cookie")
          .asScala
          .find(_.startsWith(AitSessionCookie + "="))
          .map(_.split(';').head.stripPrefix(AitSessionCookie + "=")) match {
          case Some(sessionid) =>
            request.setAttribute(AitSessionRequestAttribute, sessionid)
            Right(sessionid)
          case None =>
            Left("Login failed.")
        }
      } else {
        Left("Incorrect Username/Password.")
      }
    } finally {
      Try(ait.disconnect())
    }
  }

  def sessionId(implicit request: HttpServletRequest): Option[String] =
    Option(request.getAttribute(AitSessionRequestAttribute)).map(_.toString).orElse {
      Try {
        request.cookies
          .get(AitSessionCookie)
          .filter(_ != null)
          .map(_.trim)
          .filter(_.nonEmpty)
          .orElse {
            request
              .header("Authorization")
              .filter(_ != null)
              .map(_.trim)
              .filter(_.toLowerCase.startsWith("basic "))
              .map(StringUtil.stripPrefixBySeparator(_, " "))
              .flatMap { base64 =>
                val userPassword = new String(Base64.getDecoder.decode(base64), "utf-8")
                val Array(user, password) = userPassword.split(":", 2)
                login(user, password).right.toOption
              }
          }
      }.getOrElse(None)
    }

  def get[R](path: String, contentType: String = "text/html")(action: InputStream => Option[R])(
      implicit request: HttpServletRequest): Option[R] = {
    getWithAuth(path, contentType, sessionId)(action)
  }

  def getWithAuth[R](
      path: String,
      contentType: String = "text/html",
      sessionId: Option[String] = None,
      basicAuth: Option[String] = None)(action: InputStream => Option[R]): Option[R] = {
    val ait = new URL(
      if (path.startsWith("https:")) path
      else "https://partner.archive-it.org" + path).openConnection
      .asInstanceOf[HttpURLConnection]
    try {
      for (sid <- sessionId) ait.setRequestProperty("Cookie", AitSessionCookie + "=" + sid)
      for (auth <- basicAuth) ait.setRequestProperty("Authorization", auth)
      ait.setRequestProperty("Accept", contentType)
      val in = ait.getInputStream
      try {
        action(in)
      } finally {
        Try(in.close())
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    } finally {
      Try(ait.disconnect())
    }
  }

  def getString[R](path: String, contentType: String = "text/html")(action: String => Option[R])(
      implicit request: HttpServletRequest): Option[R] =
    getStringWithAuth(path, contentType, sessionId)(action)

  def getStringWithAuth[R](
      path: String,
      contentType: String = "text/html",
      sessionId: Option[String] = None,
      basicAuth: Option[String] = None)(action: String => Option[R]): Option[R] =
    getWithAuth(path, contentType, sessionId, basicAuth) { in =>
      val source = Source.fromInputStream(in, "utf-8")
      try {
        action(source.mkString)
      } finally {
        Try(source.close())
      }
    }

  def getJson[R](path: String)(action: HCursor => Option[R])(
      implicit request: HttpServletRequest): Option[R] = {
    getJsonWithAuth(path, sessionId)(action)
  }

  def getJsonWithAuth[R](
      path: String,
      sessionId: Option[String] = None,
      basicAuth: Option[String] = None)(action: HCursor => Option[R]): Option[R] =
    getStringWithAuth(path, "application/json", sessionId, basicAuth) { str =>
      parse(str).right.toOption.flatMap { json =>
        action(json.hcursor)
      }
    }

  def logout()(implicit request: HttpServletRequest, response: HttpServletResponse): Unit =
    get("/logout") { _ =>
      None
    }
}

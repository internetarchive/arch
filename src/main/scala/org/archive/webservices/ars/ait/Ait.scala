package org.archive.webservices.ars.ait

import io.circe.HCursor
import io.circe.parser._
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.sparkling.util.StringUtil
import org.scalatra.servlet.ServletApiImplicits._
import org.scalatra.{Cookie, CookieOptions}

import java.io.{InputStream, PrintWriter}
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.util.Base64
import javax.net.ssl.HttpsURLConnection
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Try

object Ait {
  val AitSessionRequestAttribute = "AIT-sessionid"
  val AitSessionCookie = "sessionid"
  val UserSessionAttribute = "ait-user"

  def user(implicit request: HttpServletRequest): Option[AitUser] = user(useSession = false)

  def user(useSession: Boolean)(implicit request: HttpServletRequest): Option[AitUser] = {
    (if (useSession)
       Option(request.getSession.getAttribute(UserSessionAttribute)).map(_.asInstanceOf[AitUser])
     else None).orElse {
      implicit val context = RequestContext(request)
      getJson("/api/auth/list") { json =>
        for {
          userName <- json.get[String]("username").toOption
          id <- json
            .downField("account")
            .get[Int]("id")
            .toOption
        } yield {
          val user =
            AitUser(
              id,
              userName,
              json.get[String]("full_name").toOption.filter(_.nonEmpty).getOrElse(userName),
              json.get[String]("email").toOption)
          request.getSession.setAttribute(UserSessionAttribute, user)
          user
        }
      }.toOption
    }
  }

  def user(id: Int)(implicit context: RequestContext = RequestContext.None): Option[AitUser] = {
    val foreignAuthHeader = ArchConf.foreignAitAuthHeader.filter(_ =>
      context.isInternal || (context.isAdmin && context.loggedIn.aitUser.isEmpty))
    getJsonWithAuth(
      "/api/user?limit=1&account=" + id,
      sessionId = context.forRequest(sessionId(_)),
      basicAuth = foreignAuthHeader) { json =>
      val user = json.downArray
      for {
        userName <- user.get[String]("username").toOption
      } yield
        AitUser(
          id,
          userName,
          user.get[String]("full_name").toOption.getOrElse(userName),
          user.get[String]("email").toOption)
    }.toOption
  }

  def login(username: String, password: String, response: HttpServletResponse)(
      implicit request: HttpServletRequest): Either[String, String] = {
    val either = login(username, password)
    val aitRootDomain = (new URL(ArchConf.aitBaseUrl).getHost.split('.') match {
      case xs => xs.slice(xs.length-2, xs.length)
    }).mkString(".")
    for (sessionid <- either.right.toOption)
      response.addCookie(
        Cookie(AitSessionCookie, sessionid)(
          if (ArchConf.production) CookieOptions(aitRootDomain, path = "/")
          else CookieOptions()))
    either
  }

  def login(username: String, password: String)(
      implicit request: HttpServletRequest): Either[String, String] = {
    val ait = new URL(ArchConf.aitBaseUrl + ArchConf.aitLoginPath).openConnection
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

  def sessionId(implicit request: HttpServletRequest): Option[String] = {
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
                val Array(user, password) =
                  userPassword.stripPrefix(ArchUser.AitPrefix + ":").split(":", 2)
                login(user, password).right.toOption
              }
          }
      }.getOrElse(None)
    }
  }

  def get[R](path: String, contentType: String = "text/html", basicAuth: Option[String] = None)(
      action: InputStream => Option[R])(
      implicit context: RequestContext = RequestContext.None): Either[Int, R] = {
    getWithAuth(path, contentType, context.forRequest(sessionId(_)), basicAuth)(action)
  }

  def getWithAuth[R](
      path: String,
      contentType: String = "text/html",
      sessionId: Option[String] = None,
      basicAuth: Option[String] = None,
      close: Boolean = true)(action: InputStream => Option[R]): Either[Int, R] = {
    val ait = new URL(
      if (path.matches("^https?://.+$")) path
      else ArchConf.aitBaseUrl + path).openConnection
      .asInstanceOf[HttpURLConnection]
    ait.setInstanceFollowRedirects(true)
    try {
      for (sid <- sessionId if basicAuth.isEmpty)
        ait.setRequestProperty("Cookie", AitSessionCookie + "=" + sid)
      for (auth <- basicAuth) ait.setRequestProperty("Authorization", auth)
      ait.setRequestProperty("Accept", contentType)
      val status = ait.getResponseCode
      if (status / 100 == 2) {
        val in = ait.getInputStream
        try {
          action(in) match {
            case Some(r) => Right(r)
            case None => Left(-1)
          }
        } finally {
          if (close) Try(in.close())
        }
      } else Left(status)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    } finally {
      if (close) Try(ait.disconnect())
    }
  }

  def getString[R](
      path: String,
      contentType: String = "text/html",
      basicAuth: Option[String] = None)(action: String => Option[R])(
      implicit context: RequestContext = RequestContext.None): Either[Int, R] =
    getStringWithAuth(path, contentType, context.forRequest(sessionId(_)), basicAuth)(action)

  def getStringWithAuth[R](
      path: String,
      contentType: String = "text/html",
      sessionId: Option[String] = None,
      basicAuth: Option[String] = None)(action: String => Option[R]): Either[Int, R] =
    getWithAuth(path, contentType, sessionId, basicAuth) { in =>
      val source = Source.fromInputStream(in, "utf-8")
      try {
        action(source.mkString)
      } finally {
        Try(source.close())
      }
    }

  def getJson[R](path: String, basicAuth: Option[String] = None)(action: HCursor => Option[R])(
      implicit context: RequestContext = RequestContext.None): Either[Int, R] = {
    getJsonWithAuth(path, context.forRequest(sessionId(_)), basicAuth)(action)
  }

  def getJsonWithAuth[R](
      path: String,
      sessionId: Option[String] = None,
      basicAuth: Option[String] = None)(action: HCursor => Option[R]): Either[Int, R] = {
    getStringWithAuth(path, "application/json", sessionId, basicAuth) { str =>
      parse(str).right.toOption.flatMap { json =>
        action(json.hcursor)
      }
    }
  }

  def logout()(implicit request: HttpServletRequest): Unit =
    get("/logout") { _ =>
      request.getSession.removeAttribute(UserSessionAttribute)
      None
    }
}

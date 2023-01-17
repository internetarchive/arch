package org.archive.webservices.ars

import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.scalatra._

import scala.util.Try

class BaseController extends ScalatraServlet {
  def login(url: String): ActionResult = TemporaryRedirect(ArchConf.loginUrl + url)

  def ensureLogin(action: RequestContext => ActionResult): ActionResult = ensureLogin()(action)

  def ensureLogin(
      requiresLogin: Boolean = true,
      redirect: Boolean = true,
      useSession: Boolean = false,
      validateCollection: Option[String] = None,
      userId: Option[String] = None)(
      action: RequestContext => ActionResult): ActionResult = {
    val context = ArchUser.get(useSession) match {
      case Some(loggedIn) =>
        val user = userId.flatMap(ArchUser.get).filter(u => loggedIn.isAdmin || loggedIn.id == u.id).getOrElse(loggedIn)
        RequestContext(loggedIn, user)
      case None => RequestContext(ArchUser.None)
    }
    if (requiresLogin) {
      if (context.isUser && (validateCollection.isEmpty || ArchCollection
            .get(validateCollection.get)
            .isDefined)) {
        action(context)
      } else {
        if (redirect) login(request.uri.toString) else Forbidden()
      }
    } else action(context)
  }

  def ensureUserBasePath(
      userIdKey: String,
      redirectOnForbidden: Boolean = true,
      validateCollection: Option[String] = None)(action: RequestContext => ActionResult)(
      implicit request: HttpServletRequest): ActionResult = {
    val userId = params(userIdKey)
    val userIdInt = Try(userId.toInt).toOption
    if (userId.contains(":") || userIdInt.isDefined) {
      val path = requestPath.stripPrefix("/" + userId)
      ensureLogin(redirect = redirectOnForbidden, validateCollection = validateCollection) {
        context =>
          val viewUser = if (context.isAdmin) ArchUser.get(userId) else context.loggedInOpt
          viewUser match {
            case Some(u) =>
              if (userId == u.id || u.aitUser.exists(aitUser => userIdInt.contains(aitUser.id))) {
                if (u.urlId != userId) Found(relativePath(u, path, ""))
                else action(RequestContext(context.loggedIn, u))
              } else login(Arch.BaseUrl + "/" + userId + path)
            case None =>
              NotFound()
          }
      }
    } else pass()
  }

  def relativePath(user: ArchUser, relative: String, dir: String = Arch.BaseDir): String = {
    Arch.BaseUrl + "/" + user.urlId + dir + relative
  }

  def relativePath(relative: String)(implicit context: RequestContext): String =
    relativePath(context.userOpt.getOrElse(context.loggedIn), relative, Arch.BaseDir)
}

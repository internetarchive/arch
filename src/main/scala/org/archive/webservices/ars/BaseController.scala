package org.archive.webservices.ars

import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.scalatra._

import scala.util.Try

class BaseController extends ScalatraServlet {
  def login(url: String): ActionResult = TemporaryRedirect(ArchConf.loginUrl + url)

  def ensureLogin(action: ArchUser => ActionResult): ActionResult = ensureLogin()(action)

  def ensureLogin(
      requiresLogin: Boolean = true,
      redirect: Boolean = true,
      useSession: Boolean = false,
      validateCollection: Option[String] = None)(
      action: ArchUser => ActionResult): ActionResult = {
    if (requiresLogin) {
      val user = ArchUser.get(useSession)
      if (user.isDefined && (validateCollection.isEmpty || ArchCollection
            .get(validateCollection.get)
            .isDefined)) {
        action(user.get)
      } else {
        if (redirect) login(Arch.BaseUrl + requestPath) else Forbidden()
      }
    } else action(ArchUser.NoUser)
  }

  def ensureUserBasePath(
      userIdKey: String,
      redirectOnForbidden: Boolean = true,
      validateCollection: Option[String] = None)(action: ArchUser => ActionResult)(
      implicit request: HttpServletRequest): ActionResult = {
    val userId = params(userIdKey)
    val userIdInt = Try(userId.toInt).toOption
    if (userId.contains(":") || userIdInt.isDefined) {
      val path = requestPath.stripPrefix("/" + userId)
      ensureLogin(redirect = redirectOnForbidden, validateCollection = validateCollection) {
        loggedIn =>
          (if (loggedIn.isAdmin) ArchUser.get(userId) else Some(loggedIn)) match {
            case Some(user) =>
              if (userId != user.id && !user.aitUser.exists(u => userIdInt.contains(u.id)))
                login(Arch.BaseUrl + "/" + userId + path)
              else if (user.urlId != userId)
                Found(relativePath(path, "")(user))
              else action(user)
            case None =>
              NotFound()
          }
      }
    } else pass
  }

  def relativePath(relative: String, dir: String = Arch.BaseDir)(
      implicit user: ArchUser): String = {
    Arch.BaseUrl + "/" + user.urlId + dir + relative
  }
}

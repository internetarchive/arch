package org.archive.webservices.ars

import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.ait.{Ait, AitUser}
import org.archive.webservices.ars.model.{ArsCloudCollection, ArsCloudConf}
import org.scalatra._

import scala.util.Try

class BaseController extends ScalatraServlet {
  def login(url: String): ActionResult = TemporaryRedirect(ArsCloudConf.loginUrl + url)

  def chooseAccount(): ActionResult =
    TemporaryRedirect("https://partner.archive-it.org/choose_account")

  def ensureLogin(action: AitUser => ActionResult): ActionResult = ensureLogin()(action)

  def ensureLogin(
      requiresLogin: Boolean = true,
      redirect: Boolean = true,
      useSession: Boolean = false,
      validateCollection: Option[String] = None)(
      action: AitUser => ActionResult): ActionResult = {
    if (requiresLogin) {
      val user = Ait.user(useSession)
      if (user.isDefined && (validateCollection.isEmpty || ArsCloudCollection
            .get(validateCollection.get)
            .isDefined)) {
        action(user.get)
      } else {
        if (redirect) login(ArsCloud.BaseUrl + requestPath) else Forbidden()
      }
    } else action(AitUser.None)
  }

  def ensureUserBasePath(
      userIdKey: String,
      redirectOnForbidden: Boolean = true,
      validateCollection: Option[String] = None)(action: AitUser => ActionResult)(
      implicit request: HttpServletRequest): ActionResult = {
    val userId = params(userIdKey)
    val path = requestPath.stripPrefix("/" + userId)
    Try(userId.toInt).toOption match {
      case Some(parsedId) =>
        ensureLogin(redirect = redirectOnForbidden, validateCollection = validateCollection) {
          loggedIn =>
            (if (loggedIn.id == 0) Ait.user(parsedId) else Some(loggedIn)) match {
              case Some(user) =>
                if (parsedId != user.id) login(ArsCloud.BaseUrl + "/" + parsedId + path)
                else if (userId != user.id.toString) Found(relativePath(path, "")(user))
                else action(user)
              case None =>
                chooseAccount()
            }
        }
      case None =>
        pass
    }
  }

  def relativePath(relative: String, dir: String = ArsCloud.BaseDir)(
      implicit user: AitUser): String = {
    ArsCloud.BaseUrl + "/" + user.idStr + dir + relative
  }
}

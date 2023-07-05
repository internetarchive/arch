package org.archive.webservices.ars

import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.scalatra._

class BaseController extends ScalatraServlet {
  def login(url: String): ActionResult = TemporaryRedirect(ArchConf.loginUrl + url)

  def ensureLogin(action: RequestContext => ActionResult): ActionResult = ensureLogin()(action)

  def ensureLogin(
      requiresLogin: Boolean = true,
      redirect: Boolean = true,
      useSession: Boolean = false,
      validateCollection: Option[String] = None,
      userId: Option[String] = None)(action: RequestContext => ActionResult): ActionResult = {
    val context = ArchUser.get(useSession) match {
      case Some(loggedIn) =>
        val user = userId
          .flatMap(ArchUser.get)
          .filter(u => loggedIn.isAdmin || loggedIn.id == u.id)
          .getOrElse(loggedIn)
        RequestContext(loggedIn, user)
      case None => RequestContext(ArchUser.None)
    }
    if (requiresLogin) {
      if (context.isUser && (validateCollection.isEmpty || ArchCollection
            .get(validateCollection.get)(context)
            .isDefined)) {
        action(context)
      } else {
        if (redirect) login(request.uri.toString) else Forbidden()
      }
    } else action(context)
  }
}

object BaseController {
  def relativePath(path: String): String =
    ArchConf.baseUrl + "/" + path.stripPrefix("/")

  def staticPath(path: String): String = {
    ArchConf.baseUrl + "/" + path.stripPrefix("/")
  }
}

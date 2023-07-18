package org.archive.webservices.ars

import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class BaseController extends ScalatraServlet with ScalateSupport {
  val MasqueradeUserIdSessionAttribute = "masquerade-user"

  def login(url: String): ActionResult = TemporaryRedirect(ArchConf.loginUrl + url)

  def ensureLogin(action: RequestContext => ActionResult): ActionResult = ensureLogin()(action)

  def clearMasqueradeUser(): Unit = request.getSession.removeAttribute(MasqueradeUserIdSessionAttribute)

  def masqueradeUser(userId: String): Unit = {
    if (userId.trim.isEmpty) clearMasqueradeUser()
    else request.getSession.setAttribute(MasqueradeUserIdSessionAttribute, userId)
  }

  def masqueradeUser: Option[String] = Option(request.getSession.getAttribute(MasqueradeUserIdSessionAttribute)).map(_.toString.trim).filter(_.nonEmpty)

  def ensureLogin(
      requiresLogin: Boolean = true,
      redirect: Boolean = true,
      useSession: Boolean = false,
      validateCollection: Option[String] = None)(action: RequestContext => ActionResult): ActionResult = {
    val context = ArchUser.get(useSession) match {
      case Some(loggedIn) =>
        val user = masqueradeUser
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

  def ssp(path: String, attributes: (String, Any)*)(implicit request: HttpServletRequest, response: HttpServletResponse, context: RequestContext = RequestContext(request)): String = {
    super.ssp(path, attributes ++ Seq("requestContext" -> context): _*)
  }
}

object BaseController {
  def relativePath(path: String): String =
    ArchConf.baseUrl + "/" + path.stripPrefix("/")

  def staticPath(path: String): String = {
    ArchConf.baseUrl + "/" + path.stripPrefix("/")
  }
}

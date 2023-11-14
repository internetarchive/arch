package org.archive.webservices.ars

import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class BaseController extends ScalatraServlet with ScalateSupport {
  // Report and rethrow any Exceptions.
  error {
    case e: Exception => {
      Arch.reportException(e)
      throw e
    }
  }

  def ensureAuth(action: RequestContext => ActionResult): ActionResult = {
    for {
      apiUser <- request.headers.get("X-API-USER")
      apiKey <- request.headers.get("X-API-KEY")
    } yield {
      ArchUser.get(apiUser, Some(apiKey)) match {
        case Some(user) => action(RequestContext(user))
        case None => Forbidden()
      }
    }
  }.getOrElse(Forbidden())
}

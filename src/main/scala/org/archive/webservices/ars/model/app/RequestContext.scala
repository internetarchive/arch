package org.archive.webservices.ars.model.app

import org.archive.webservices.ars.model.users.ArchUser

import javax.servlet.http.HttpServletRequest

class RequestContext private (
    val request: Option[HttpServletRequest],
    val loggedIn: ArchUser,
    val user: ArchUser) {
  def isRequest: Boolean = request.nonEmpty
  def isInternal: Boolean = !isRequest
  def isUser: Boolean = loggedIn.isUser
  def loggedInOpt: Option[ArchUser] = loggedIn.option
  def userOpt: Option[ArchUser] = user.option
  def isAdmin: Boolean = loggedIn.isAdmin
  def forRequest[R](action: HttpServletRequest => Option[R]): Option[R] = request.flatMap(action)
}

object RequestContext {
  val None: RequestContext = new RequestContext(scala.None, ArchUser.None, ArchUser.None)

  def apply(
      request: Option[HttpServletRequest],
      loggedIn: ArchUser,
      user: ArchUser): RequestContext = {
    new RequestContext(request, loggedIn, user)
  }
  def apply(loggedIn: ArchUser, user: ArchUser)(implicit
      request: HttpServletRequest): RequestContext = {
    RequestContext(Some(request), loggedIn, user)
  }
  def apply(user: ArchUser)(implicit request: HttpServletRequest): RequestContext = {
    RequestContext(user, user)
  }
  def apply(user: Option[ArchUser])(implicit request: HttpServletRequest): RequestContext = {
    RequestContext(user.getOrElse(ArchUser.None))
  }

  def apply(request: HttpServletRequest): RequestContext = {
    new RequestContext(Some(request), ArchUser.None, ArchUser.None)
  }
}

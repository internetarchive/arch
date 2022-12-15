package org.archive.webservices.ars.model.app

import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.model.users.ArchUser

class RequestContext private (
    val request: Option[HttpServletRequest],
    val user: ArchUser,
    val viewUser: ArchUser) {
  def isRequest: Boolean = request.nonEmpty
  def isInternal: Boolean = !isRequest
  def isUser: Boolean = user.isUser
  def userOpt: Option[ArchUser] = user.option
  def viewUserOpt: Option[ArchUser] = viewUser.option
  def isAdmin: Boolean = user.isAdmin
  def forRequest[R](action: HttpServletRequest => Option[R]): Option[R] = request.flatMap(action)
}

object RequestContext {
  val None: RequestContext = new RequestContext(scala.None, ArchUser.None, ArchUser.None)

  def apply(
      request: Option[HttpServletRequest],
      user: ArchUser,
      viewUser: ArchUser): RequestContext = {
    new RequestContext(request, user, viewUser)
  }
  def apply(user: ArchUser, viewUser: ArchUser)(
      implicit request: HttpServletRequest): RequestContext = {
    RequestContext(Some(request), user, viewUser)
  }
  def apply(user: ArchUser)(implicit request: HttpServletRequest): RequestContext = {
    RequestContext(user, user)
  }
  def apply(user: Option[ArchUser])(implicit request: HttpServletRequest): RequestContext = {
    RequestContext(user.getOrElse(ArchUser.None))
  }
}

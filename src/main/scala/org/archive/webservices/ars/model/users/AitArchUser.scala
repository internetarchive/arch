package org.archive.webservices.ars.model.users

import org.archive.webservices.ars.ait.AitUser

class AitArchUser private (wrapped: AitUser) extends ArchUser {
  override def id: String =
    if (wrapped.isLoggedIn) ArchUser.AitPrefix + ":" + wrapped.id else ""
  override def userName: String = ArchUser.AitPrefix + ":" + wrapped.userName
  override def fullName: String = wrapped.fullName
  override def email: Option[String] = wrapped.email
  override def isAdmin: Boolean = wrapped.isSystemUser
  override def isLoggedIn: Boolean = wrapped.isLoggedIn
  override val aitUser: Option[AitUser] = Some(wrapped)
}

object AitArchUser {
  def apply(wrapped: AitUser): ArchUser = new AitArchUser(wrapped)
}

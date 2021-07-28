package org.archive.webservices.ars.ait

case class AitUser(id: Int, userName: String, fullName: String) {
  def isSystemUser: Boolean = id == 0
  def isLoggedIn: Boolean = id >= 0
}

object AitUser {
  lazy val None = AitUser(-1, "", "")
}

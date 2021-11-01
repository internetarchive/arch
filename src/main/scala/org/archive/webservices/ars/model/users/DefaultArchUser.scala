package org.archive.webservices.ars.model.users

case class DefaultArchUser(
    id: String,
    userName: String,
    fullName: String,
    email: Option[String],
    isAdmin: Boolean,
    isLoggedIn: Boolean = true)
    extends ArchUser

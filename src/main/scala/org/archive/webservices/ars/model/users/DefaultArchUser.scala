package org.archive.webservices.ars.model.users

case class DefaultArchUser(
    id: String,
    userName: String,
    fullName: String,
    email: Option[String],
    isAdmin: Boolean,
    isUser: Boolean = true)
    extends ArchUser

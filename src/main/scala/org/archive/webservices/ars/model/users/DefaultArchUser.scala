package org.archive.webservices.ars.model.users

case class DefaultArchUser(
    id: String,
    userName: String,
    fullName: String,
    isAdmin: Boolean,
    isLoggedIn: Boolean = true)
    extends ArchUser

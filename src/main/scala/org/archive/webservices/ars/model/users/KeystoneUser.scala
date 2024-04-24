package org.archive.webservices.ars.model.users

import io.circe.parser
import io.circe.syntax._
import org.archive.webservices.ars.model.ArchConf
import requests._

object KeystoneUser {
  val prefix = "ks"

  private def parseKeystoneUserResponse(r: Response): Option[DefaultArchUser] =
    if (r.statusCode != 200) None
    else {
      parser.parse(r.text) match {
        case Left(error) =>
          None
        case Right(json) =>
          val cursor = json.hcursor
          Some(
            DefaultArchUser(
              id = prefix + ":" + cursor.get[String]("username").toOption.get,
              userName = cursor.get[String]("username").toOption.get,
              fullName = cursor.get[String]("fullname").toOption.get,
              email = cursor.get[String]("email").toOption,
              isAdmin = cursor.get[Boolean]("is_staff").toOption.get))
      }
    }

  def get(username: String): Option[DefaultArchUser] =
    if (ArchConf.keystoneBaseUrl.isEmpty || ArchConf.keystonePrivateApiKey.isEmpty) {
      None
    } else {
      parseKeystoneUserResponse(
        requests.get(
          s"${ArchConf.keystoneBaseUrl.get}/private/api/user?username=${username}",
          headers = Map("X-API-Key" -> ArchConf.keystonePrivateApiKey.get),
          check = false))
    }

  def login(username: String, password: String): Option[DefaultArchUser] =
    if (ArchConf.keystoneBaseUrl.isEmpty || ArchConf.keystonePrivateApiKey.isEmpty) {
      None
    } else {
      parseKeystoneUserResponse(
        requests.post(
          s"${ArchConf.keystoneBaseUrl.get}/private/api/proxy_login",
          data = Map("username" -> username, "password" -> password).asJson.noSpaces,
          headers = Map("X-API-Key" -> ArchConf.keystonePrivateApiKey.get),
          check = false))
    }

}

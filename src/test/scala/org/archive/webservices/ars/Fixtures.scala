package test.org.archive.webservices.ars

import java.io.{File, PrintWriter}

import scala.io.Source

import io.circe.Json
import io.circe.syntax._
import io.circe.parser.parse

import org.archive.webservices.sparkling.util.DigestUtil

import org.archive.webservices.ars.model.users.ArchUser

object Fixtures {
  private def load(path: String): Json = {
    val source = Source.fromFile(path)
    val json = parse(source.getLines.mkString).right.get
    source.close()
    json
  }

  private def store(path: String, json: Json): Unit = {
    val source = Source.fromFile(path)
    val pw = new PrintWriter(new File(path))
    pw.write(json.toString)
    pw.close()
  }

  def makeArchUser(admin: Boolean = false): ArchUser = {
    // Insert a randomly-generated user into the arch-users.json file and
    // return the corresponding ArchUser instance.
    val path = "data/arch-users.json"
    val json = load(path)
    var userId = java.time.Instant.now.toEpochMilli.toString
    // In the event of a collision, append a "0".
    val existingUserIds = json.hcursor.keys.get.toSet
    while (existingUserIds.contains(userId)) {
      userId += "0"
    }
    store(path, json.deepMerge(Map(
      userId -> Map(
        "name" -> userId.asJson,
        "password" -> s"sha1:${DigestUtil.sha1Base32(userId)}".asJson,
        "admin" -> admin.asJson
      )).asJson
    ))
    ArchUser.invalidateData()
    ArchUser.get(s"arch:$userId").get
  }
}

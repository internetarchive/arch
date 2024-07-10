package org.archive.webservices.ars.util

import io.circe.parser.parse
import io.circe.{Json, JsonObject}

import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.Try

object DatafileUtil {
  private def getPath(filename: String) = s"data/$filename"

  def load(filename: String): Json = {
    val source = Source.fromFile(getPath(filename), "utf-8")
    Try {
      try {
        parse(source.getLines.mkString).right.get
      } finally {
        source.close()
      }
    }.getOrElse(Json.fromJsonObject(JsonObject.empty))
  }

  def store(filename: String, json: Json): Unit = {
    val path = getPath(filename)
    val source = Source.fromFile(path, "utf-8")
    try {
      val pw = new PrintWriter(new File(path))
      pw.write(json.toString)
      pw.close()
    } finally {
      source.close()
    }
  }

  def loadArchUsers() = load("arch-users.json")
  def storeArchUsers(json: Json) = store("arch-users.json", json)
}

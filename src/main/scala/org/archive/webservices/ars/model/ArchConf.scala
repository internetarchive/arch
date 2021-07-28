package org.archive.webservices.ars.model

import _root_.io.circe.{Json, JsonObject, parser}
import org.archive.webservices.ars.Arch

import scala.io.Source
import scala.util.Try

object ArchConf {
  val ConfFile = "config/config.json"

  private val cursor = Try {
    val source = Source.fromFile(ConfFile, "utf-8")
    try {
      parser.parse(source.mkString).right.get.hcursor
    } finally {
      source.close()
    }
  }.getOrElse(Json.fromJsonObject(JsonObject.empty).hcursor)

  lazy val aitCollectionHdfsHostPort: Option[(String, Int)] =
    cursor
      .get[String]("aitCollectionHdfsHost")
      .toOption
      .map((_, cursor.get[Int]("aitCollectionHdfsPort").toOption.getOrElse(6000)))

  lazy val aitCollectionPath: String =
    cursor.get[String]("aitCollectionPath").toOption.getOrElse("data/in")

  lazy val aitCollectionWarcDir: String =
    cursor.get[String]("aitCollectionWarcDir").toOption.getOrElse("arcs")

  lazy val collectionCachePath: String =
    cursor.get[String]("collectionCachePath").toOption.getOrElse("/data/cache")

  lazy val jobOutPath: String = cursor.get[String]("jobOutPath").toOption.getOrElse("data/out")

  lazy val localTempPath: String =
    cursor.get[String]("localTempPath").toOption.getOrElse("data/tmp")

  lazy val sparkMaster: String = cursor.get[String]("sparkMaster").toOption.getOrElse("local[*]")

  lazy val baseUrl: String =
    cursor.get[String]("baseUrl").toOption.getOrElse("http://127.0.0.1:" + Arch.Port)

  lazy val loginUrl: String = cursor
    .get[String]("loginUrl")
    .toOption
    .getOrElse("http://127.0.0.1:" + Arch.Port + "/ait/login?next=")

  lazy val hadoopQueue: String =
    cursor.get[String]("hadoopQueue").toOption.getOrElse("default")

  lazy val production: Boolean =
    cursor.get[Boolean]("production").toOption.getOrElse(false)

  lazy val port: Int =
    cursor.get[Int]("port").toOption.getOrElse(12341)

  /** python:
   * import requests, base64
   * base64.b64encode("user:pass".encode())
    **/
  lazy val foreignAitAuthHeader: Option[String] =
    cursor.get[String]("foreignAitAuthHeader").toOption.map("Basic " + _)
}

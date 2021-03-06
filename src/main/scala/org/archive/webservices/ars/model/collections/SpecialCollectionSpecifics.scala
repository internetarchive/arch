package org.archive.webservices.ars.model.collections

import java.io.InputStream

import io.circe.{HCursor, Json, JsonObject, parser}
import javax.servlet.http.HttpServletRequest
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.ars.io.CollectionLoader
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.users.ArchUser

import scala.io.Source
import scala.util.Try

class SpecialCollectionSpecifics(id: String) extends CollectionSpecifics {
  val specialId: String = id.stripPrefix(SpecialCollectionSpecifics.Prefix)

  def inputPath: String =
    SpecialCollectionSpecifics
      .collectionInfo(specialId)
      .flatMap(_.get[String]("path").toOption)
      .get

  def getCollection(implicit request: HttpServletRequest): Option[ArchCollection] =
    ArchUser.get
      .filter(u =>
        u.isAdmin || SpecialCollectionSpecifics.userCollectionIds(u).contains(specialId))
      .flatMap(_ => SpecialCollectionSpecifics.get(specialId))

  def size(implicit request: HttpServletRequest): Long =
    HdfsIO.fs.getContentSummary(new Path(inputPath)).getLength

  def seeds(implicit request: HttpServletRequest): Int = -1

  def lastCrawlDate(implicit request: HttpServletRequest): String = ""

  def loadWarcFiles(inputPath: String): RDD[(String, InputStream)] =
    CollectionLoader.loadWarcFiles(inputPath)
}

object SpecialCollectionSpecifics {
  val Prefix = "SPECIAL-"

  private var _collectionsCursor: Option[HCursor] = None
  private def collectionsCursor: HCursor = _collectionsCursor.getOrElse {
    _collectionsCursor = Some(Try {
      val source = Source.fromFile("data/special-collections.json", "utf-8")
      try {
        parser.parse(source.mkString).right.get.hcursor
      } finally {
        source.close()
      }
    }.getOrElse(Json.fromJsonObject(JsonObject.empty).hcursor))
    _collectionsCursor.get
  }

  def invalidateData(): Unit = _collectionsCursor = None

  private def collectionInfo(id: String): Option[HCursor] = {
    collectionsCursor
      .downField("collections")
      .downField(id)
      .focus
      .map(_.hcursor)
  }

  def userCollectionIds(user: ArchUser): Seq[String] = {
    collectionsCursor
      .downField("users")
      .downField(user.id)
      .values
      .toSeq
      .flatten
      .flatMap(_.asString)
  }

  def get(id: String): Option[ArchCollection] = {
    collectionInfo(id)
      .filter(_.get[String]("path").toOption.map(_.trim).exists(_.nonEmpty))
      .map { c =>
        ArchCollection(Prefix + id, c.get[String]("name").toOption.getOrElse(id), public = false)
      }
  }

  def userCollections(user: ArchUser): Seq[ArchCollection] = {
    userCollectionIds(user)
      .flatMap(get)
  }
}

package org.archive.webservices.ars.model.collections

import io.circe.{HCursor, Json, JsonObject, parser}
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{FileAccessContext, FilePointer, WebArchiveLoader}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats}
import org.archive.webservices.sparkling.io.HdfsIO

import java.io.InputStream
import scala.io.Source
import scala.util.Try

class SpecialCollectionSpecifics(val id: String) extends CollectionSpecifics {
  val (userId, specialId) =
    ArchCollection.splitIdUserCollection(id.stripPrefix(SpecialCollectionSpecifics.Prefix))

  def inputPath: String =
    SpecialCollectionSpecifics
      .collectionInfo(specialId)
      .flatMap(_.get[String]("path").toOption)
      .get

  def collection(implicit
      context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    if (context.isInternal || context.loggedInOpt.exists { u =>
        u.isAdmin || SpecialCollectionSpecifics.userCollectionIds(u).contains(specialId)
      }) SpecialCollectionSpecifics.collection(specialId, userId)
    else None
  }

  override def stats(implicit
      context: RequestContext = RequestContext.None): ArchCollectionStats = {
    ArchCollectionStats(HdfsIO.fs.getContentSummary(new Path(inputPath)).getLength)
  }

  def loadWarcFiles[R](inputPath: String)(action: RDD[(FilePointer, InputStream)] => R): R = {
    val sourceId = this.sourceId
    WebArchiveLoader.loadWarcFiles(inputPath) { rdd =>
      action(rdd.map { case (filename, in) =>
        (CollectionSpecifics.pointer(sourceId, filename), in)
      })
    }
  }

  def randomAccess(
      context: FileAccessContext,
      inputPath: String,
      pointer: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    WebArchiveLoader.randomAccessHdfs(
      context,
      inputPath + "/" + pointer.filename,
      offset,
      positions)
  }

  override def sourceId: String = SpecialCollectionSpecifics.Prefix + specialId
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
      .downField(id.stripPrefix(Prefix))
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

  def collection(id: String, user: ArchUser): Option[ArchCollection] =
    collection(id, Some(user.id))

  def collection(id: String, user: Option[String] = None): Option[ArchCollection] = {
    val idWithoutPrefix = id.stripPrefix(Prefix)
    collectionInfo(idWithoutPrefix)
      .filter(_.get[String]("path").toOption.map(_.trim).exists(_.nonEmpty))
      .map { c =>
        ArchCollection(
          ArchCollection.prependUserId(id, user, Prefix),
          c.get[String]("name").toOption.getOrElse(idWithoutPrefix),
          public = false,
          user.map((_, Prefix + idWithoutPrefix)),
          Prefix + idWithoutPrefix)
      }
  }

  def userCollections(user: ArchUser): Seq[ArchCollection] =
    userCollectionIds(user).flatMap(collection(_, user))
}

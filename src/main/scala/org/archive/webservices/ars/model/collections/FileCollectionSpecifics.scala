package org.archive.webservices.ars.model.collections

import io.circe.{HCursor, Json, JsonObject, parser}
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{FileAccessContext, FilePointer, IOHelper}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.inputspecs.{FileRecordFactory, InputSpec, InputSpecLoader}
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats}
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.IOUtil

import java.io.InputStream
import scala.io.Source
import scala.util.Try

class FileCollectionSpecifics(val id: String)
    extends CollectionSpecifics
    with GenericRandomAccess {
  val (userId, collectionId) =
    ArchCollection.splitIdUserCollection(id.stripPrefix(FileCollectionSpecifics.Prefix))

  def inputPath: String =
    FileCollectionSpecifics
      .collectionInfo(collectionId)
      .flatMap(_.focus.map(_.noSpacesSortKeys))
      .get

  def collection(implicit
      context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    if (context.isInternal || context.loggedInOpt.exists { u =>
        u.isAdmin || FileCollectionSpecifics.userCollectionIds(u).contains(collectionId)
      }) FileCollectionSpecifics.collection(collectionId, userId)
    else None
  }

  override def stats(implicit
      context: RequestContext = RequestContext.None): ArchCollectionStats = {
    ArchCollectionStats(
      FileCollectionSpecifics
        .collectionInfo(collectionId)
        .flatMap(_.get[Long]("size").toOption)
        .getOrElse(-1))
  }

  def loadWarcFiles[R](inputPath: String)(action: RDD[(FilePointer, InputStream)] => R): R = {
    InputSpecLoader.load(InputSpec(inputPath)) { rdd =>
      action(
        rdd
          .filter { file =>
            val filename = file.filename.toLowerCase.stripSuffix(Sparkling.GzipExt)
            filename.endsWith(Sparkling.ArcExt) || filename.endsWith(Sparkling.WarcExt)
          }
          .map(f => (f.pointer, f.access)))
    }
  }

  private val factories = scala.collection.mutable.Map.empty[String, FileRecordFactory]
  override def randomAccess(
                             context: FileAccessContext,
                             inputPath: String,
                             pointer: FilePointer,
                             offset: Long,
                             positions: Iterator[(Long, Long)]): InputStream = {
    if (pointer.source != sourceId)
      return super.randomAccess(context, inputPath, pointer, offset, positions)
    val spec = InputSpec(inputPath)
    val factory = factories.getOrElseUpdate(inputPath, FileRecordFactory(spec))
    val in = factory.accessFile(pointer.filename, accessContext = context)
    IOUtil.skip(in, offset)
    IOHelper.splitMergeInputStreams(in, positions, buffered = false)
  }

  override def sourceId: String = FileCollectionSpecifics.Prefix + collectionId
}

object FileCollectionSpecifics {
  val Prefix = "FILES-"

  private var _collectionsCursor: Option[HCursor] = None
  private def collectionsCursor: HCursor = _collectionsCursor.getOrElse {
    _collectionsCursor = Some(Try {
      val source = Source.fromFile("data/file-collections.json", "utf-8")
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

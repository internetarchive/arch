package org.archive.webservices.ars.model.collections

import io.circe._
import org.apache.http.MethodNotSupportedException
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{CollectionAccessContext, CollectionLoader, CollectionSourcePointer}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.archive.webservices.sparkling.cdx.{CdxLoader, CdxRecord}
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.StringUtil

import java.io.InputStream
import scala.util.Try

class CustomCollectionSpecifics(val id: String) extends CollectionSpecifics {
  val customId: String = id.stripPrefix(CustomCollectionSpecifics.Prefix)
  val Some((userId, collectionId)) = ArchCollection.splitIdUserCollectionOpt(customId)

  def inputPath: String =
    CustomCollectionSpecifics
      .path(customId)
      .get

  def collection(
      implicit context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    if (context.isInternal || context.loggedInOpt.exists { u =>
          u.isAdmin || CustomCollectionSpecifics.userCollectionIds(u).contains(customId)
        }) CustomCollectionSpecifics.collection(customId)
    else None
  }

  def size(implicit context: RequestContext = RequestContext.None): Long = {
    CustomCollectionSpecifics
      .collectionInfo(customId)
      .flatMap { info =>
        info.get[Long]("size").toOption
      }
      .getOrElse(0L)
  }

  def seeds(implicit context: RequestContext = RequestContext.None): Int = -1

  def lastCrawlDate(implicit context: RequestContext = RequestContext.None): String = ""

  def loadWarcFiles[R](inputPath: String)(action: RDD[(String, InputStream)] => R): R = action {
    CustomCollectionSpecifics
      .collectionInfo(customId)
      .flatMap(_.get[String]("location").toOption) match {
      case Some(location) =>
        val cdxPath = inputPath + "/" + CustomCollectionSpecifics.CdxDir
        val locationId = StringUtil
          .prefixBySeparator(location.toLowerCase, CustomCollectionSpecifics.LocationIdSeparator)
        locationId match {
          case "petabox" =>
            CollectionLoader.loadWarcFilesViaCdxFromPetabox(cdxPath)
          case "hdfs" | "ait-hdfs" =>
            val warcPath = StringUtil
              .stripPrefixBySeparator(location, CustomCollectionSpecifics.LocationIdSeparator)
            CollectionLoader
              .loadWarcFilesViaCdxFromHdfs(cdxPath, warcPath, aitHdfs = locationId == "ait-hdfs")
          case "arch" | _ =>
            val parentCollectionId =
              if (locationId == "arch")
                StringUtil
                  .stripPrefixBySeparator(location, CustomCollectionSpecifics.LocationIdSeparator)
              else location
            CollectionLoader.loadWarcFilesViaCdxFromCollections(cdxPath, parentCollectionId)
        }
      case None =>
        throw new MethodNotSupportedException("Unknown location for collection " + id)
    }
  }

  override def loadCdx[R](inputPath: String)(action: RDD[CdxRecord] => R): R = {
    val cdxPath = inputPath + "/" + CustomCollectionSpecifics.CdxDir
    action(CdxLoader.load(s"$cdxPath/*.cdx.gz"))
  }

  def randomAccess(
      context: CollectionAccessContext,
      inputPath: String,
      pointer: CollectionSourcePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    CollectionLoader.randomAccessHdfs(
      context,
      inputPath + "/" + pointer.filename,
      offset,
      positions)
  }
}

object CustomCollectionSpecifics {
  val Prefix = "CUSTOM-"
  val InfoFile = "info.json"
  val LocationIdSeparator = ":"
  val CdxDir = "index.cdx.gz"

  private def collectionInfo(id: String): Option[HCursor] = path(id).flatMap { path =>
    val infoPath = path + s"/$InfoFile"
    if (HdfsIO.exists(infoPath)) {
      val str = HdfsIO.lines(infoPath).mkString
      Try(parser.parse(str).right.get.hcursor).toOption
    } else None
  }

  def userPath(userId: String): String =
    ArchConf.customCollectionPath + "/" + userId.replace(
      ArchCollection.UserIdSeparator,
      ArchCollection.PathUserEscape)

  def path(user: ArchUser): String = userPath(user.id)

  def path(id: String): Option[String] = {
    ArchCollection
      .splitIdUserCollectionOpt(id.stripPrefix(Prefix))
      .map {
        case (user, collection) =>
          val p = userPath(user)
          s"$p/$collection"
      }
      .filter(HdfsIO.exists)
  }

  def userCollectionIds(user: ArchUser): Seq[String] = {
    HdfsIO
      .files(path(user) + "/*", recursive = false)
      .filter(p => HdfsIO.exists(p + s"/$InfoFile"))
      .flatMap(_.stripSuffix("/").split('/').lastOption)
      .toSeq
      .map { id =>
        user.id + ArchCollection.UserIdSeparator + id
      }
  }

  def collection(id: String): Option[ArchCollection] = {
    val idWithoutPrefix = id.stripPrefix(Prefix)
    collectionInfo(idWithoutPrefix).map { info =>
      ArchCollection(
        Prefix + idWithoutPrefix,
        info.get[String]("name").toOption.getOrElse(Prefix + idWithoutPrefix),
        public = false,
        ArchCollection.splitIdUserCollectionOpt(id, Prefix),
        Prefix + idWithoutPrefix)
    }
  }

  def userCollections(user: ArchUser): Seq[ArchCollection] =
    userCollectionIds(user).flatMap(collection)
}

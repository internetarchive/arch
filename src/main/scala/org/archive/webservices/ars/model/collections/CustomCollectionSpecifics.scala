package org.archive.webservices.ars.model.collections

import io.circe._
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.WebArchiveLoader
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.collections.inputspecs.FilePointer
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionStats, ArchConf}
import org.archive.webservices.ars.util.CacheUtil
import org.archive.webservices.sparkling.cdx.{CdxLoader, CdxRecord}
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.StringUtil

import java.io.InputStream
import scala.util.Try

class CustomCollectionSpecifics(val id: String) extends CollectionSpecifics with GenericRandomAccess {
  val customId: String = id.stripPrefix(CustomCollectionSpecifics.Prefix)
  val Some((userId, collectionId)) = ArchCollection.splitIdUserCollectionOpt(customId)

  def inputPath: String =
    CustomCollectionSpecifics
      .path(customId)
      .get

  def collection(implicit
      context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    if (context.isInternal || context.loggedInOpt.exists { u =>
        u.isAdmin || CustomCollectionSpecifics.userCollectionIds(u).contains(customId)
      }) CustomCollectionSpecifics.collection(customId)
    else None
  }

  override def stats: ArchCollectionStats = {
    var stats = ArchCollectionStats.Empty
    val size = CustomCollectionSpecifics
      .collectionInfo(customId)
      .flatMap { info =>
        info.get[Long]("size").toOption
      }
    for (s <- size) stats = stats.copy(size = s)
    stats
  }

  def loadWarcFiles[R](inputPath: String)(action: RDD[(FilePointer, InputStream)] => R): R = {
    val sourceId = this.sourceId
    action({
      val cdxPath = inputPath + "/" + CustomCollectionSpecifics.CdxDir
      CustomCollectionSpecifics.location(customId) match {
        case Some(location) =>
          val locationId = StringUtil
            .prefixBySeparator(location.toLowerCase, CustomCollectionSpecifics.LocationIdSeparator)
          locationId match {
            case "petabox" =>
              WebArchiveLoader.loadWarcFilesViaCdxFromPetabox(cdxPath)
            case "hdfs" | "ait-hdfs" =>
              val warcPath = StringUtil
                .stripPrefixBySeparator(location, CustomCollectionSpecifics.LocationIdSeparator)
              WebArchiveLoader
                .loadWarcFilesViaCdxFromHdfs(cdxPath, warcPath, aitHdfs = locationId == "ait-hdfs")
            case "arch" | _ =>
              val parentCollectionId =
                if (locationId == "arch")
                  StringUtil
                    .stripPrefixBySeparator(location, CustomCollectionSpecifics.LocationIdSeparator)
                else location
              WebArchiveLoader.loadWarcFilesViaCdxFromCollections(cdxPath, parentCollectionId)
          }
        case None => WebArchiveLoader.loadWarcFilesViaCdxFiles(cdxPath)
      }
    }.map{case (filename, in) => (CollectionSpecifics.pointer(sourceId, filename), in)})
  }

  override def loadCdx[R](inputPath: String)(action: RDD[CdxRecord] => R): R = {
    val cdxPath = inputPath + "/" + CustomCollectionSpecifics.CdxDir
    val locationPrefix = CustomCollectionSpecifics
      .location(customId)
      .map(_ + FilePointer.SourceSeparator)
      .getOrElse("")
    val cdx = CdxLoader.load(s"$cdxPath/*.cdx.gz").map { r =>
      val Seq(offsetStr, filename) = r.additionalFields
      if (filename.contains(FilePointer.SourceSeparator)) r
      else r.copy(additionalFields = Seq(offsetStr, locationPrefix + filename))
    }
    action(cdx)
  }
}

object CustomCollectionSpecifics {
  val Prefix = "CUSTOM-"
  val InfoFile = "info.json"
  val LocationIdSeparator = ":"
  val CdxDir = "index.cdx.gz"

  private def collectionInfo(id: String): Option[HCursor] = path(id).flatMap { path =>
    CacheUtil.cache[Option[HCursor]](s"CustomCollectionSpecifics:collectionInfo:$path") {
      val infoPath = path + s"/$InfoFile"
      if (HdfsIO.exists(infoPath)) {
        val str = HdfsIO.lines(infoPath).mkString
        Try(parser.parse(str).right.get.hcursor).toOption
      } else None
    }
  }

  def location(id: String): Option[String] =
    collectionInfo(id).flatMap(_.get[String]("location").toOption)

  def userPath(userId: String): String =
    ArchConf.customCollectionPath + "/" + userId.replace(
      ArchCollection.UserIdSeparator,
      ArchCollection.PathUserEscape)

  def path(user: ArchUser): String = userPath(user.id)

  def path(id: String): Option[String] = {
    ArchCollection
      .splitIdUserCollectionOpt(id.stripPrefix(Prefix))
      .map { case (user, collection) =>
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

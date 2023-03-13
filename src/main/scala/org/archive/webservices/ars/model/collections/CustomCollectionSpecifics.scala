package org.archive.webservices.ars.model.collections

import io.circe._
import org.apache.http.MethodNotSupportedException
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.CollectionLoader
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.StringUtil

import java.io.InputStream
import scala.util.Try

class CustomCollectionSpecifics(val id: String) extends CollectionSpecifics {
  val customId: String = id.stripPrefix(CustomCollectionSpecifics.Prefix)
  val Some((userId, collectionId)) = ArchCollection.splitIdUserCollection(customId)

  def inputPath: String =
    CustomCollectionSpecifics
      .path(customId)
      .get

  def collection(
      implicit context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    if (context.isInternal || context.loggedInOpt.exists { u =>
          u.isAdmin || CustomCollectionSpecifics.userCollectionIds(u).contains(customId)
        }) CustomCollectionSpecifics.get(customId)
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

  def loadWarcFiles(inputPath: String): RDD[(String, InputStream)] = {
    CustomCollectionSpecifics
      .collectionInfo(customId)
      .flatMap(_.get[String]("location").toOption) match {
      case Some(location) =>
        val cdxPath = inputPath + "/" + CustomCollectionSpecifics.CdxDir
        val locationId = StringUtil.prefixBySeparator(
          location.toLowerCase,
          CustomCollectionSpecifics.LocationIdSeparator)
        locationId match {
          case "petabox" =>
            val warcPath = StringUtil.stripPrefixBySeparator(
              location,
              CustomCollectionSpecifics.LocationIdSeparator)
            CollectionLoader.loadWarcFilesViaCdxFromPetabox(cdxPath, warcPath)
          case "hdfs" | "ait-hdfs" =>
            val warcPath = StringUtil.stripPrefixBySeparator(
              location,
              CustomCollectionSpecifics.LocationIdSeparator)
            CollectionLoader.loadWarcFilesViaCdxFromHdfs(
              cdxPath,
              warcPath,
              aitHdfs = locationId == "ait-hdfs")
          case "arch" | _ =>
            val parentCollectionId = StringUtil.stripPrefixBySeparator(
              location,
              CustomCollectionSpecifics.LocationIdSeparator)
            CollectionSpecifics
              .get(parentCollectionId)
              .map { parent =>
                if (parentCollectionId.startsWith(AitCollectionSpecifics.Prefix)) {
                  CollectionLoader.loadWarcFilesViaCdxFromAit(
                    cdxPath,
                    parent.inputPath,
                    parent.cacheId)
                } else CollectionLoader.loadWarcFilesViaCdxFromHdfs(cdxPath, parent.inputPath)
              }
              .getOrElse {
                throw new MethodNotSupportedException("Unknown location " + location)
              }
        }
      case None =>
        throw new MethodNotSupportedException("Unknown location for collection " + id)
    }
  }

  override def jobOutPath: String = userId + "/" + CustomCollectionSpecifics.Prefix + collectionId
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
    ArchConf.customCollectionPath + "/" + userId.replace(ArchCollection.UserIdSeparator, ArchCollection.PathUserEscape)

  def path(user: ArchUser): String = userPath(user.id)

  def path(id: String): Option[String] = {
    ArchCollection.splitIdUserCollection(id)
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

  def get(id: String): Option[ArchCollection] = {
    collectionInfo(id).map { info =>
      ArchCollection(
        Prefix + id,
        info.get[String]("name").toOption.getOrElse(Prefix + id),
        public = false,
        ArchCollection.splitIdUserCollection(id).map(Prefix + _._2))
    }
  }

  def userCollections(user: ArchUser): Seq[ArchCollection] =
    userCollectionIds(user).flatMap(get)
}

package org.archive.webservices.ars.model.collections

import java.io.InputStream

import io.circe._
import org.apache.http.MethodNotSupportedException
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.CollectionLoader
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf}
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.{RddUtil, StringUtil}

import scala.util.Try

class CustomCollectionSpecifics(id: String) extends CollectionSpecifics {
  val customId: String = id.stripPrefix(CustomCollectionSpecifics.Prefix)

  def inputPath: String =
    CustomCollectionSpecifics
      .path(customId)
      .get

  def collection(
      implicit context: RequestContext = RequestContext.None): Option[ArchCollection] = {
    if (context.isInternal || context.userOpt.exists { u =>
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
        val locationId = StringUtil.prefixBySeparator(
          location.toLowerCase,
          CustomCollectionSpecifics.LocationIdSeparator)
        locationId match {
          case "petabox" =>
            val warcPath = StringUtil.stripPrefixBySeparator(
              location,
              CustomCollectionSpecifics.LocationIdSeparator)
            CollectionLoader.loadWarcFilesViaCdxFromPetabox(inputPath, warcPath)
          case "hdfs" | "ait-hdfs" =>
            val warcPath = StringUtil.stripPrefixBySeparator(
              location,
              CustomCollectionSpecifics.LocationIdSeparator)
            CollectionLoader.loadWarcFilesViaCdxFromHdfs(
              inputPath,
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
                  val aitId = parent.asInstanceOf[AitCollectionSpecifics].aitId
                  CollectionLoader.loadWarcFilesViaCdxFromAit(
                    inputPath,
                    parent.inputPath,
                    parentCollectionId)
                } else CollectionLoader.loadWarcFilesViaCdxFromHdfs(inputPath, parent.inputPath)
              }
              .getOrElse {
                throw new MethodNotSupportedException("Unknown location " + location)
              }
        }
      case None =>
        throw new MethodNotSupportedException("Unknown location for collection " + id)
    }
  }
}

object CustomCollectionSpecifics {
  val Prefix = "CUSTOM-"
  val InfoFile = "info.json"
  val UserIdSeparator = ":"
  val PathUserEscape = "-"
  val LocationIdSeparator = ":"

  private def collectionInfo(id: String): Option[HCursor] = path(id).flatMap { path =>
    val str = HdfsIO.lines(path + s"/$InfoFile").mkString
    Try { parser.parse(str).right.get.hcursor }.toOption
  }

  def splitIdUserCollection(id: String): Option[(String, String)] = {
    val split = id.split(UserIdSeparator)
    if (split.length > 2) Some {
      val collection = split.last
      val user = split.dropRight(1).mkString(UserIdSeparator)
      (user, collection)
    } else None
  }

  def userPath(userId: String): String =
    ArchConf.customCollectionPath + "/" + userId.replace(":", PathUserEscape)

  def path(user: ArchUser): String = userPath(user.id)

  def path(id: String): Option[String] = {
    splitIdUserCollection(id)
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
      .flatMap(_.stripSuffix("/").split('/').lastOption)
      .toSeq
      .map { id =>
        user.id + UserIdSeparator + id
      }
  }

  def id(id: String, user: ArchUser = ArchUser.None): String = {
    val (prefix, c) = if (id.startsWith(Prefix)) (Prefix, id.stripPrefix(Prefix)) else ("", id)
    prefix + (splitIdUserCollection(c) match {
      case Some(_) => c
      case None => user.id + UserIdSeparator + c
    })
  }

  def get(id: String, user: ArchUser = ArchUser.None): Option[ArchCollection] = {
    collectionInfo(id).map { info =>
      ArchCollection(
        Prefix + id,
        info.get[String]("name").toOption.getOrElse(Prefix + id),
        public = false,
        splitIdUserCollection(id).map(Prefix + _._2))
    }
  }

  def userCollections(user: ArchUser): Seq[ArchCollection] =
    userCollectionIds(user).flatMap(get(_))
}

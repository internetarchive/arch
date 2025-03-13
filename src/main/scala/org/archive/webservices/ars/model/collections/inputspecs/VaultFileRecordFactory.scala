package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.Vault.TreeNode
import org.archive.webservices.ars.io._
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaData

import java.io.InputStream
import java.net.URL
import scala.util.Try

class VaultFileRecordFactory(
    location: String,
    collectionTreenode: Int,
    username: String,
    sessionId: String,
    longestPrefixMapping: Boolean,
    defaultGlob: Option[String])
    extends FileRecordFactory
    with LongestPrefixProbing {
  def companion = VaultFileRecordFactory

  class VaultFileRecord private[VaultFileRecordFactory] (
      file: String,
      val mime: String,
      val meta: FileMetaData)
      extends FileRecord {
    override lazy val filePath: String = locateFile(file)
    lazy val url: String = contentUrl(filePath, resolve = false)
    override def access: InputStream = accessContentUrl(url)
    override def pointer: FilePointer = {
      // for later, when changed to Vault content URL instead of public archive.org
      // FilePointer(IOHelper.insertUrlUser(url, username), filename)
      FilePointer(url, filename)
    }
  }

  override def get(file: String, mime: String, meta: FileMetaData): FileRecord =
    new VaultFileRecord(file, mime, meta)

  def accessFile(
      file: String,
      resolve: Boolean = true,
      accessContext: FileAccessContext): InputStream = {
    accessContentUrl(contentUrl(file, resolve = resolve))
  }

  def accessContentUrl(contentUrl: String): InputStream = {
    // for later when changed to Vault content URL instead of public archive.org
    // Vault.access(contentUrl, sessionId)
    new URL(contentUrl).openStream
  }

  def contentUrl(file: String, resolve: Boolean = true): String = {
    val path = if (resolve) locateFile(file) else file
    Vault.treeNode(sessionId, collectionTreenode, path).flatMap(_.contentUrl).get
  }

  def iterateFiles(): (Set[(String, TreeNode)], Set[String]) = {
    iterateGlob(Set(defaultGlob.getOrElse("**")))
  }

  def iterateGlob(glob: Set[String]): (Set[(String, TreeNode)], Set[String]) = {
    val (resolved, remaining) = Vault.iterateGlob(
      sessionId,
      collectionTreenode,
      glob.map(IOHelper.concatPaths(location, _)))
    (
      resolved.map { case (p, n) => (p.stripPrefix(location + "/"), n) },
      remaining.map(_.stripPrefix(location + "/")))
  }

  def glob(glob: String): Iterator[(String, TreeNode)] = {
    Vault.glob(sessionId, collectionTreenode, glob).map { case (p, n) =>
      (p.stripPrefix(location + "/"), n)
    }
  }

  def locateFile(file: String): String = if (file.startsWith("/")) file
  else
    FileRecordFactory.filePath(
      {
        if (longestPrefixMapping) IOHelper.concatPaths(location, locateLongestPrefixPath(file))
        else location
      },
      file)

  private val prefixes = collection.mutable.Map.empty[String, Set[String]]
  protected def nextPrefixes(prefix: String): Set[String] = {
    prefixes.getOrElseUpdate(
      prefix, {
        Vault
          .children(sessionId, collectionTreenode, IOHelper.concatPaths(location, prefix))
          .map { node =>
            IOHelper.concatPaths(prefix, node.name)
          }
          .toSet
      })
  }
}

object VaultFileRecordFactory extends FileFactoryCompanion {
  val VaultUrl = "https://vault.archive-it.org"

  val dataSourceType: String = "vault"

  def apply(spec: InputSpec): VaultFileRecordFactory = {
    val (dataUser, dataPw, dataCollection, dataLocation, dataGlob) = spec.str(InputSpec.DataLocationKey).map { str =>
      val atIdx = str.indexOf('@')
      val (user, pw, rest) = if (atIdx >= 0) {
        val userPw = str.take(atIdx)
        val colonIdx = userPw.indexOf(':')
        if (colonIdx >= 0) {
          (Some(userPw.take(colonIdx)), Some(userPw.drop(colonIdx + 1)), str.drop(atIdx + 1))
        } else (Some(userPw), None, str.drop(atIdx + 1))
      } else (None, None, str)
      val colonIdx = rest.indexOf(':')
      val (treeNode, path) = if (colonIdx >= 0) {
        (Try(rest.take(colonIdx).toInt).toOption, rest.drop(colonIdx + 1))
      } else (None, rest)
      val (location, glob) = if (path.contains("*")) {
        val split = path.split('/')
        val location = split.takeWhile(!_.contains("*"))
        (Some(location.mkString("/")), Some(split.drop(location.length).mkString("/")))
      } else (Some(path), None)
      (user, pw, treeNode, location, glob)
    }.getOrElse((None, None, None, None, None))

    val userOpt = spec.str("vaultUsername").orElse(dataUser)
    for {
      collectionTreenode <- spec
        .str("vaultCollectionTreenode")
        .flatMap(id => Try(id.toInt).toOption).orElse(dataCollection)
      (username, password) <- FileAccessKeyRing
        .forUrl(VaultUrl)
        .flatMap {
          case (FileAccessKeyRing.AccessMethodVault, Array(user, pw)) =>
            Some((user, pw))
          case (FileAccessKeyRing.AccessMethodVault, Array(pw)) =>
            userOpt.map((_, pw))
          case _ => None
        }
        .orElse {
          for {
            user <- userOpt
            pw <- spec.str("vaultPassword").orElse(dataPw)
          } yield (user, pw)
        }
    } yield {
      val longestPrefixMapping = spec.str("dataPathMapping").contains("longest-prefix")
      new VaultFileRecordFactory(
        dataLocation.getOrElse(""),
        collectionTreenode,
        username,
        Vault.session(username, password),
        longestPrefixMapping,
        spec.str("fileGlob").orElse(dataGlob))
    }
  }.getOrElse {
    throw new RuntimeException("No location URL specified.")
  }
}

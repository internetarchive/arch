package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.Vault.TreeNode
import org.archive.webservices.ars.io._

import java.io.InputStream
import java.net.URL
import scala.util.Try

class VaultFileRecordFactory(
    location: String,
    collectionTreenode: Int,
    username: String,
    sessionId: String,
    longestPrefixMapping: Boolean)
    extends FileRecordFactory
    with LongestPrefixProbing {
  def companion = VaultFileRecordFactory

  class VaultFileRecord private[VaultFileRecordFactory] (
      file: String,
      val mime: String,
      val meta: FileMeta)
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

  override def get(file: String, mime: String, meta: FileMeta): FileRecord =
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
    val userOpt = spec.str("vault-username")
    for {
      collectionTreenode <- spec
        .str("vault-collection-treenode")
        .flatMap(id => Try(id.toInt).toOption)
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
            pw <- spec.str("vault-password")
          } yield (user, pw)
        }
    } yield {
      val longestPrefixMapping = spec.str("data-path-mapping").contains("longest-prefix")
      new VaultFileRecordFactory(
        spec.str(InputSpec.DataLocationKey).getOrElse(""),
        collectionTreenode,
        username,
        Vault.session(username, password),
        longestPrefixMapping)
    }
  }.getOrElse {
    throw new RuntimeException("No location URL specified.")
  }
}

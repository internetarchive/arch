package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io._

import java.io.InputStream
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
      val filename: String,
      val mime: String,
      val meta: FileMeta)
      extends FileRecord {
    override lazy val path: String = locatePath(filename)
    lazy val url: String = contentUrl(filePath, resolve = false)
    override def access: InputStream = Vault.access(url, sessionId)
    override def pointer: FilePointer = {
      // for later when changed to Vault content URL instead of archive.org
      //FilePointer(IOHelper.insertUrlUser(url, username), filename)
      FilePointer(url, filename)
    }
  }

  override def get(filename: String, mime: String, meta: FileMeta): FileRecord =
    new VaultFileRecord(filename, mime, meta)

  def accessFile(
      filePath: String,
      resolve: Boolean = true,
      accessContext: FileAccessContext): InputStream = {
    Vault.access(contentUrl(filePath, resolve = resolve), sessionId)
  }

  def contentUrl(filePath: String, resolve: Boolean = true): String = {
    val path = if (resolve) FileRecordFactory.filePath(locatePath(filePath), filePath) else filePath
    Vault.contentUrl(sessionId, collectionTreenode, path).get
  }

  def locatePath(filename: String): String = {
    if (longestPrefixMapping) IOHelper.concatPaths(location, locateLongestPrefixPath(filename))
    else location
  }

  private val prefixes = collection.mutable.Map.empty[String, Set[String]]
  protected def nextPrefixes(prefix: String): Set[String] = {
    prefixes.getOrElseUpdate(prefix, {
      Vault.children(sessionId, collectionTreenode, IOHelper.concatPaths(location, prefix)).map { case (name, _) =>
        IOHelper.concatPaths(prefix, name)
      }.toSet
    })
  }
}

object VaultFileRecordFactory extends FileFactoryCompanion {
  val VaultUrl = "https://vault.archive-it.org"

  val dataSourceType: String = "vault"

  def apply(spec: InputSpec): VaultFileRecordFactory = {
    val userOpt = spec.str("vault-username")
    for {
      collectionTreenode <- spec.str("vault-collection-treenode").flatMap(id => Try(id.toInt).toOption)
      (username, password) <- FileAccessKeyRing.forUrl(VaultUrl).flatMap {
        case (FileAccessKeyRing.AccessMethodVault, Array(user, pw)) =>
          Some((user, pw))
        case (FileAccessKeyRing.AccessMethodVault, Array(pw)) =>
          userOpt.map((_, pw))
        case _ => None
      }.orElse {
        for {
          user <- userOpt
          pw <- spec.str("vault-password")
        } yield (user, pw)
      }
      location <- spec.str(InputSpec.DataLocationKey)
    } yield {
      val longestPrefixMapping = spec.str("data-path-mapping").contains("longest-prefix")
      new VaultFileRecordFactory(
        location,
        collectionTreenode,
        username,
        Vault.session(username, password),
        longestPrefixMapping)
    }
  }.getOrElse {
    throw new RuntimeException("No location URL specified.")
  }
}

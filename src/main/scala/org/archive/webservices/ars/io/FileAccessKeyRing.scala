package org.archive.webservices.ars.io

import scala.collection.JavaConverters._

class FileAccessKeyRing private (secrets: Map[String, String]) extends Serializable {
  import FileAccessKeyRing._

  def forUrl(url: String): Option[(String, Array[String])] = {
    val (protocol, path) = {
      val splitAt = url.lastIndexOf(":")
      if (splitAt < 0) ("", url) else (url.take(splitAt), url.drop(splitAt + 1))
    }
    val secretSplit = path
      .split('/')
      .find(_.nonEmpty)
      .flatMap { host =>
        secrets.get(secretKey(protocol, host))
      }
      .toArray
      .flatMap(_.split(SecretSeparator))
    secretSplit.headOption.filter(SupportedAccessMethods.contains).map((_, secretSplit.drop(1)))
  }
}

object FileAccessKeyRing {
  val SecretSeparator = "::"
  val AccessMethodS3 = "s3"
  val SupportedAccessMethods = Set(AccessMethodS3)
  val SecretEnvPrefix = "ARCH_SECRET_"

  def secretKey(protocol: String, host: String): String = {
    s"${protocol.toUpperCase}_${host.replace('.', '-').toUpperCase}"
  }

  lazy val system: FileAccessKeyRing = {
    new FileAccessKeyRing(
      System.getenv().asScala.toMap.filterKeys(_.startsWith(SecretEnvPrefix)).map { case (k, v) =>
        k.stripPrefix(SecretEnvPrefix) -> v
      })
  }

  def forUrl(url: String): Option[(String, Array[String])] = system.forUrl(url)
}

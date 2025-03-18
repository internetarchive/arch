package org.archive.webservices.ars.io

import org.archive.webservices.sparkling.io.IOUtil

import java.io.File
import scala.collection.JavaConverters._

class FileAccessKeyRing private (secrets: Map[String, String]) extends Serializable {
  import FileAccessKeyRing._

  def forUrl(url: String): Option[(String, Array[String])] = {
    val uri = new java.net.URI(url)
    val protocol = uri.getScheme
    val path = uri.getPath
    val secretSplit = secrets.get(secretKey(uri.getScheme, uri.getHost))
      .toArray
      .flatMap(_.split(SecretSeparator))
    secretSplit.headOption.filter(SupportedAccessMethods.contains).map((_, secretSplit.drop(1)))
  }
}

object FileAccessKeyRing {
  val SecretSeparator = "::"
  val AccessMethodS3 = "s3"
  val AccessMethodBasic = "basic"
  val AccessMethodVault = "vault"
  val SupportedAccessMethods = Set(AccessMethodS3, AccessMethodBasic, AccessMethodVault)
  val SecretEnvPrefix = "ARCH_SECRET_"
  val SecretsFile = ".secrets"

  def secretKey(protocol: String, host: String): String = {
    s"${protocol.toUpperCase}_${host.replace('.', '-').toUpperCase}"
  }

  def loadEnv: Map[String, String] = {
    System.getenv().asScala.toMap.filterKeys(_.startsWith(SecretEnvPrefix)).map { case (k, v) =>
      k.stripPrefix(SecretEnvPrefix) -> v
    }
  }

  def loadSecrets: Map[String, String] = {
    if (new File(SecretsFile).exists) {
      IOUtil
        .lines(SecretsFile)
        .flatMap { line =>
          val equalIdx = line.indexOf("=")
          if (equalIdx == -1) None
          else
            Some {
              line.take(equalIdx).trim -> line.drop(equalIdx + 1).trim
            }
        }
        .filter { case (k, v) =>
          k.nonEmpty && v.nonEmpty
        }
        .toMap
    } else Map.empty
  }

  lazy val system: FileAccessKeyRing = new FileAccessKeyRing(loadEnv ++ loadSecrets)

  def forUrl(url: String): Option[(String, Array[String])] = system.forUrl(url)
}

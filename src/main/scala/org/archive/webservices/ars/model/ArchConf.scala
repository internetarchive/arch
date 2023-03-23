package org.archive.webservices.ars.model

import _root_.io.circe.{Json, JsonObject, parser}
import org.archive.webservices.ars.Arch

import scala.io.Source
import scala.util.Try

object ArchConf {
  val ConfFile = "config/config.json"

  private val cursor = Try {
    val source = Source.fromFile(ConfFile, "utf-8")
    try {
      parser.parse(source.mkString).right.get.hcursor
    } finally {
      source.close()
    }
  }.getOrElse(Json.fromJsonObject(JsonObject.empty).hcursor)

  private def getConfValue[A: _root_.io.circe.Decoder](
      envKey: String,
      configKey: String,
      default: A,
      parse: String => A): A =
    Option(System.getenv(envKey))
      .flatMap(x => Try(parse(x)).toOption)
      .getOrElse(cursor.get[A](configKey).toOption.getOrElse(default))

  /** Getter for String-type config values that prioritizes environment overrides **/
  private def getConfStrValue(envKey: String, configKey: String, default: String): String =
    getConfValue(envKey, configKey, default, identity)

  /** Getter for Integer-type config values that prioritizes environment overrides **/
  private def getConfIntValue(envKey: String, configKey: String, default: Int): Int =
    getConfValue(envKey, configKey, default, _.toInt)

  /** Getter for Bool-type config values that prioritizes environment overrides **/
  private def getConfBoolValue(envKey: String, configKey: String, default: Boolean): Boolean =
    getConfValue(envKey, configKey, default, _.toBoolean)

  lazy val iaBaseUrl: String =
    getConfStrValue("ARCH_IA_BASE_URL", "iaBaseUrl", "https://archive.org")

  lazy val aitCollectionHdfsHost: Option[String] =
    Option(getConfStrValue("ARCH_AIT_COLLECTION_HDFS_HOST", "aitCollectionHdfsHost", ""))
      .filter(_.nonEmpty)

  lazy val aitCollectionHdfsPort: Int =
    getConfIntValue("ARCH_AIT_COLLECTION_HDFS_PORT", "aitCollectionHdfsPort", 6000)

  lazy val aitCollectionHdfsHostPort: Option[(String, Int)] =
    aitCollectionHdfsHost.map((_, aitCollectionHdfsPort))

  lazy val aitCollectionPath: String =
    getConfStrValue("ARCH_AIT_COLLECTION_PATH", "aitCollectionPath", "data/in")

  lazy val aitCollectionWarcDir: String =
    getConfStrValue("ARCH_AIT_COLLECTION_WARC_DIR", "aitCollectionWarcDir", "arcs")

  lazy val aitBaseUrl: String =
    getConfStrValue("ARCH_AIT_BASE_URL", "aitBaseUrl", "https://partner.archive-it.org")

  lazy val aitLoginPath: String = getConfStrValue("ARCH_AIT_LOGIN_PATH", "aitLoginPath", "/login")

  lazy val aitWarcsBaseUrl: String =
    getConfStrValue("ARCH_AIT_WARCS_BASE_URL", "aitWarcsBaseUrl", "https://warcs.archive-it.org")

  lazy val collectionCachePath: String =
    getConfStrValue("ARCH_COLLECTION_CACHE_PATH", "collectionCachePath", "/data/cache")

  lazy val jobOutPath: String = getConfStrValue("ARCH_JOB_OUTPUT_PATH", "jobOutPath", "data/out")

  lazy val jobLoggingPath: String =
    getConfStrValue("ARCH_JOB_LOGGING_PATH", "jobLoggingPath", "/var/log/arch")

  lazy val customCollectionPath: String =
    getConfStrValue("ARCH_CUSTOM_COLLECTIONS_PATH", "customCollectionPath", "data/collections")

  lazy val localTempPath: String =
    getConfStrValue("ARCH_LOCAL_TEMP_PATH", "localTempPath", "data/tmp")

  lazy val sparkMaster: String = getConfStrValue("ARCH_SPARK_MASTER", "sparkMaster", "local[*]")

  lazy val baseUrl: String =
    getConfStrValue("ARCH_BASE_URL", "baseUrl", "http://127.0.0.1:" + Arch.Port)

  lazy val loginUrl: String = getConfStrValue(
    "ARCH_LOGIN_URL",
    "loginUrl",
    "http://127.0.0.1:" + Arch.Port + "/ait/login?next=")

  lazy val hadoopQueue: String = getConfStrValue("ARCH_HADOOP_QUEUE", "hadoopQueue", "default")

  lazy val production: Boolean = getConfBoolValue("ARCH_PRODUCTION", "production", false)

  lazy val port: Int = getConfIntValue("ARCH_PORT", "port", 12341)

  /** python:
   * import requests, base64
   * base64.b64encode("user:pass".encode())
    **/
  lazy val foreignAitAuthHeader: Option[String] =
    cursor
      .get[String]("foreignAitAuthHeader")
      .toOption
      .map("Basic " + _)
      .orElse(Option(System.getenv("ARCH_AIT_AUTH_HEADER")))

  /**
   * LOW s3accessKey:s3secretKey
   */
  lazy val iaAuthHeader: Option[String] =
    cursor
      .get[String]("iaS3AuthHeader")
      .toOption
      .map("LOW " + _)
      .orElse(Option(System.getenv("ARCH_IA_AUTH_HEADER")))
}

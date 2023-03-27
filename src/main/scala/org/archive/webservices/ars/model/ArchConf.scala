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

  private def confValueMap[A: _root_.io.circe.Decoder](
      envKey: String,
      configKey: String,
      parseEnv: Option[String] => Option[A])(mapConf: A => A): Option[A] =
    parseEnv(Option(System.getenv(envKey)).filter(_.nonEmpty))
      .orElse(cursor.get[A](configKey).toOption.map(mapConf))

  private def confValue[A: _root_.io.circe.Decoder](
      envKey: String,
      configKey: String,
      parseEnv: Option[String] => Option[A]): Option[A] = confValueMap(envKey, configKey, parseEnv)(identity)

  /** Getter for String-type config values that prioritizes environment overrides **/
  private def confStrValue(envKey: String, configKey: String): Option[String] =
    confValue(envKey, configKey, identity)

  /** Getter for Integer-type config values that prioritizes environment overrides **/
  private def confIntValue(envKey: String, configKey: String, default: Int): Int =
    confValue(envKey, configKey, _.flatMap(str => Try(str.toInt).toOption)).getOrElse(default)

  /** Getter for Bool-type config values that prioritizes environment overrides **/
  private def confBoolValue(envKey: String, configKey: String, default: Boolean): Boolean =
    confValue(envKey, configKey, _.flatMap(str => Try(str.toBoolean).toOption)).getOrElse(default)

  lazy val iaBaseUrl: String =
    confStrValue("ARCH_IA_BASE_URL", "iaBaseUrl").getOrElse("https://archive.org")

  lazy val aitCollectionHdfsHost: Option[String] =
    confStrValue("ARCH_AIT_COLLECTION_HDFS_HOST", "aitCollectionHdfsHost")

  lazy val aitCollectionHdfsPort: Int =
    confIntValue("ARCH_AIT_COLLECTION_HDFS_PORT", "aitCollectionHdfsPort", 6000)

  lazy val aitCollectionHdfsHostPort: Option[(String, Int)] =
    aitCollectionHdfsHost.map((_, aitCollectionHdfsPort))

  lazy val aitCollectionPath: String =
    confStrValue("ARCH_AIT_COLLECTION_PATH", "aitCollectionPath").getOrElse("data/in")

  lazy val aitCollectionWarcDir: String =
    confStrValue("ARCH_AIT_COLLECTION_WARC_DIR", "aitCollectionWarcDir").getOrElse("arcs")

  lazy val aitBaseUrl: String =
    confStrValue("ARCH_AIT_BASE_URL", "aitBaseUrl").getOrElse("https://partner.archive-it.org")

  lazy val aitLoginPath: String =
    confStrValue("ARCH_AIT_LOGIN_PATH", "aitLoginPath").getOrElse("/login")

  lazy val aitWarcsBaseUrl: String =
    confStrValue("ARCH_AIT_WARCS_BASE_URL", "aitWarcsBaseUrl").getOrElse(
      "https://warcs.archive-it.org")

  lazy val collectionCachePath: String =
    confStrValue("ARCH_COLLECTION_CACHE_PATH", "collectionCachePath").getOrElse("/data/cache")

  lazy val globalJobOutPath: String =
    confStrValue("ARCH_GLOBAL_JOB_OUTPUT_PATH", "globalJobOutPath").getOrElse("data/out")

  lazy val jobOutPath: String =
    confStrValue("ARCH_JOB_OUTPUT_PATH", "jobOutPath").getOrElse("data/user-out")

  lazy val jobLoggingPath: String =
    confStrValue("ARCH_JOB_LOGGING_PATH", "jobLoggingPath").getOrElse("/var/log/arch")

  lazy val customCollectionPath: String =
    confStrValue("ARCH_CUSTOM_COLLECTIONS_PATH", "customCollectionPath").getOrElse(
      "data/collections")

  lazy val localTempPath: String =
    confStrValue("ARCH_LOCAL_TEMP_PATH", "localTempPath").getOrElse("data/tmp")

  lazy val sparkMaster: String =
    confStrValue("ARCH_SPARK_MASTER", "sparkMaster").getOrElse("local[*]")

  lazy val baseUrl: String =
    confStrValue("ARCH_BASE_URL", "baseUrl").getOrElse("http://127.0.0.1:" + Arch.Port)

  lazy val loginUrl: String = confStrValue("ARCH_LOGIN_URL", "loginUrl").getOrElse(
    "http://127.0.0.1:" + Arch.Port + "/ait/login?next=")

  lazy val hadoopQueue: String =
    confStrValue("ARCH_HADOOP_QUEUE", "hadoopQueue").getOrElse("default")

  lazy val production: Boolean = confBoolValue("ARCH_PRODUCTION", "production", false)

  lazy val port: Int = confIntValue("ARCH_PORT", "port", 12341)

  /** python:
   * import requests, base64
   * base64.b64encode("user:pass".encode())
    **/
  lazy val foreignAitAuthHeader: Option[String] =
    confValueMap("ARCH_AIT_AUTH_HEADER", "foreignAitAuthHeader", identity)("Basic " + _)

  /**
   * LOW s3accessKey:s3secretKey
   */
  lazy val iaAuthHeader: Option[String] =
    confValueMap("ARCH_IA_AUTH_HEADER", "iaS3AuthHeader", identity)("LOW " + _)

  lazy val githubBearer: Option[String] = confStrValue("ARCH_GITHUB_BEARER", "githubBearer")
}

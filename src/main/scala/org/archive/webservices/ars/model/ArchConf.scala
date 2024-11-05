package org.archive.webservices.ars.model

trait ArchConf {
  def iaBaseUrl: String
  def aitCollectionHdfsHost: Option[String]
  def aitCollectionHdfsPort: Int
  def aitCollectionHdfsHostPort: Option[(String, Int)] =
    aitCollectionHdfsHost.map((_, aitCollectionHdfsPort))
  def aitCollectionPath: String
  def aitCollectionWarcDir: String
  def aitBaseUrl: String
  def aitLoginPath: String
  def aitWarcsBaseUrl: String
  def waybackBaseUrl: String
  def vaultBaseUrl: String
  def collectionCachePath: String
  def globalJobOutPath: String
  def jobOutPath: String
  def uuidJobOutPath: Option[String]
  def jobLoggingPath: String
  def customCollectionPath: String
  def localTempPath: String
  def sparkMaster: String
  def baseDir: String
  def basePath: String
  def baseUrl: String
  def loginUrl: String
  def hadoopQueue: String
  def proto: String
  def host: String
  def externalPort: Int
  def internalPort: Int
  def foreignAitAuthHeader: Option[String]
  def iaAuthHeader: Option[String]
  def githubBearer: Option[String]
  def arkMintBearer: Option[String]
  def pboxCollection: String
  def arkMintUrl: String
  def pboxS3Url: String
  def keystoneBaseUrl: Option[String]
  def keystonePublicBaseUrl: Option[String]
  def keystonePrivateApiKey: Option[String]
  def version: Option[String]
  def sentryDsn: String
  def deploymentEnvironment: String
  def isDev: Boolean
  def forceKeystoneLogin: Boolean
  def publicSuffixListUrl: String
}

object ArchConf extends ArchConf {
  private var instance: Option[ArchConf] = None
  def set(conf: ArchConf): Unit = instance = Some(conf)
  def conf: ArchConf = instance.getOrElse(LocalArchConf.instance)
  def iaBaseUrl: String = conf.iaBaseUrl
  def aitCollectionHdfsHost: Option[String] = conf.aitCollectionHdfsHost
  def aitCollectionHdfsPort: Int = conf.aitCollectionHdfsPort
  def aitCollectionPath: String = conf.aitCollectionPath
  def aitCollectionWarcDir: String = conf.aitCollectionWarcDir
  def aitBaseUrl: String = conf.aitBaseUrl
  def aitLoginPath: String = conf.aitLoginPath
  def aitWarcsBaseUrl: String = conf.aitWarcsBaseUrl
  def waybackBaseUrl: String = conf.waybackBaseUrl
  def vaultBaseUrl: String = conf.vaultBaseUrl
  def collectionCachePath: String = conf.collectionCachePath
  def globalJobOutPath: String = conf.globalJobOutPath
  def jobOutPath: String = conf.jobOutPath
  def uuidJobOutPath: Option[String] = conf.uuidJobOutPath
  def jobLoggingPath: String = conf.jobLoggingPath
  def customCollectionPath: String = conf.customCollectionPath
  def localTempPath: String = conf.localTempPath
  def sparkMaster: String = conf.sparkMaster
  def baseDir: String = conf.baseDir
  def basePath: String = conf.basePath
  def baseUrl: String = conf.baseUrl
  def loginUrl: String = conf.loginUrl
  def hadoopQueue: String = conf.hadoopQueue
  def proto: String = conf.proto
  def host: String = conf.host
  def externalPort: Int = conf.externalPort
  def internalPort: Int = conf.internalPort
  def foreignAitAuthHeader: Option[String] = conf.foreignAitAuthHeader
  def iaAuthHeader: Option[String] = conf.iaAuthHeader
  def githubBearer: Option[String] = conf.githubBearer
  def arkMintBearer: Option[String] = conf.arkMintBearer
  def pboxCollection: String = conf.pboxCollection
  def arkMintUrl: String = conf.arkMintUrl
  def pboxS3Url: String = conf.pboxS3Url
  def keystoneBaseUrl: Option[String] = conf.keystoneBaseUrl
  def keystonePublicBaseUrl: Option[String] = conf.keystonePublicBaseUrl
  def keystonePrivateApiKey: Option[String] = conf.keystonePrivateApiKey
  def version: Option[String] = conf.version
  def sentryDsn: String = conf.sentryDsn
  def deploymentEnvironment: String = conf.deploymentEnvironment
  def isDev: Boolean = conf.isDev
  def forceKeystoneLogin: Boolean = conf.forceKeystoneLogin
  def publicSuffixListUrl: String = conf.publicSuffixListUrl
}

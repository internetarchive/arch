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
  def collectionCachePath: String
  def globalJobOutPath: String
  def jobOutPath: String
  def jobLoggingPath: String
  def customCollectionPath: String
  def localTempPath: String
  def sparkMaster: String
  def baseUrl: String
  def loginUrl: String
  def hadoopQueue: String
  def production: Boolean
  def port: Int
  def foreignAitAuthHeader: Option[String]
  def iaAuthHeader: Option[String]
  def githubBearer: Option[String]
  def arkMintBearer: Option[String]
  def pboxCollection: String
  def arkMintUrl: String
  def pboxS3Url: String
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
  def collectionCachePath: String = conf.collectionCachePath
  def globalJobOutPath: String = conf.globalJobOutPath
  def jobOutPath: String = conf.jobOutPath
  def jobLoggingPath: String = conf.jobLoggingPath
  def customCollectionPath: String = conf.customCollectionPath
  def localTempPath: String = conf.localTempPath
  def sparkMaster: String = conf.sparkMaster
  def baseUrl: String = conf.baseUrl
  def loginUrl: String = conf.loginUrl
  def hadoopQueue: String = conf.hadoopQueue
  def production: Boolean = conf.production
  def port: Int = conf.port
  def foreignAitAuthHeader: Option[String] = conf.foreignAitAuthHeader
  def iaAuthHeader: Option[String] = conf.iaAuthHeader
  def githubBearer: Option[String] = conf.githubBearer
  def arkMintBearer: Option[String] = conf.arkMintBearer
  def pboxCollection: String = conf.pboxCollection
  def arkMintUrl: String = conf.arkMintUrl
  def pboxS3Url: String = conf.pboxS3Url
}

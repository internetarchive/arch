package org.archive.webservices.ars.io

import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.io.HdfsIO

class CollectionAccessContext(val aitHdfsHostPort: Option[(String, Int)], val aitAuth: Option[String], val cachePath: String, val iaAuthHeader: Option[String] = None, alwaysAitHdfsIO: Boolean = false) {
  lazy val hdfsIO: HdfsIO = if (alwaysAitHdfsIO) aitHdfsIO else HdfsIO
  lazy val aitHdfsIO: HdfsIO = aitHdfsHostPort.map { case (host, port) => HdfsIO(host, port) }.getOrElse(HdfsIO)
  def cachePath(sourceId: String): String = cachePath + "/" + CollectionCache.cacheDir(sourceId)
  def cachePath(sourceId: String, filename: String): String = cachePath(sourceId) + "/" + filename
}

object CollectionAccessContext {
  def fromArchConf: CollectionAccessContext = new CollectionAccessContext(
    aitHdfsHostPort = ArchConf.aitCollectionHdfsHostPort,
    aitAuth = ArchConf.foreignAitAuthHeader,
    cachePath = ArchConf.collectionCachePath,
    iaAuthHeader = ArchConf.iaAuthHeader)
  def fromArchConf(alwaysAitHdfsIO: Boolean) = new CollectionAccessContext(
    aitHdfsHostPort = ArchConf.aitCollectionHdfsHostPort,
    aitAuth = ArchConf.foreignAitAuthHeader,
    cachePath = ArchConf.collectionCachePath,
    iaAuthHeader = ArchConf.iaAuthHeader,
    alwaysAitHdfsIO = alwaysAitHdfsIO)
}
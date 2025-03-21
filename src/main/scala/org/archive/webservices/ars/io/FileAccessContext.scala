package org.archive.webservices.ars.io

import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.{ArchConf, LocalArchConf}
import org.archive.webservices.sparkling.io.HdfsIO

class FileAccessContext(
    val conf: ArchConf with Serializable,
    val useAitHdfsIO: Boolean = false,
    val keyRing: FileAccessKeyRing,
    val keyValueCache: Map[String, java.io.Serializable])
    extends Serializable {
  @transient lazy val hdfsIO: HdfsIO = if (useAitHdfsIO) aitHdfsIO else HdfsIO
  @transient lazy val aitHdfsIOopt: Option[HdfsIO] =
    conf.aitCollectionHdfsHostPort
      .map { case (host, port) => HdfsIO(host, port) }

  def aitHdfsIO: HdfsIO = aitHdfsIOopt.getOrElse(hdfsIO)

  @transient private var initialized: Boolean = false
  def init(): Unit = if (!initialized) {
    initialized = true
    ArchConf.set(conf)
    Arch.initSentry()
  }
}

object FileAccessContext {
  var KeyValueCache = Map.empty[String, java.io.Serializable]

  def fromLocalArchConf: FileAccessContext =
    new FileAccessContext(
      conf = LocalArchConf.instance,
      keyRing = FileAccessKeyRing.system,
      keyValueCache = KeyValueCache)

  def fromLocalArchConf(alwaysAitHdfsIO: Boolean) =
    new FileAccessContext(
      conf = LocalArchConf.instance,
      useAitHdfsIO = alwaysAitHdfsIO,
      keyRing = FileAccessKeyRing.system,
      keyValueCache = KeyValueCache)
}

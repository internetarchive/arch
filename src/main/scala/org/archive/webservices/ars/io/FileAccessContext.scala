package org.archive.webservices.ars.io

import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.{ArchConf, LocalArchConf}
import org.archive.webservices.sparkling.io.HdfsIO

class FileAccessContext(
    val conf: ArchConf with Serializable,
    val useAitHdfsIO: Boolean = false,
    val keyRing: FileAccessKeyRing)
    extends Serializable {
  @transient lazy val hdfsIO: HdfsIO = if (useAitHdfsIO) aitHdfsIO.get else HdfsIO
  @transient lazy val aitHdfsIO: Option[HdfsIO] =
    conf.aitCollectionHdfsHostPort
      .map { case (host, port) => HdfsIO(host, port) }

  @transient private var initialized: Boolean = false
  def init(): Unit = if (!initialized) {
    initialized = true
    ArchConf.set(conf)
    Arch.initSentry()
  }
}

object FileAccessContext {
  def fromLocalArchConf: FileAccessContext =
    new FileAccessContext(conf = LocalArchConf.instance, keyRing = FileAccessKeyRing.system)
  def fromLocalArchConf(alwaysAitHdfsIO: Boolean) =
    new FileAccessContext(
      conf = LocalArchConf.instance,
      useAitHdfsIO = alwaysAitHdfsIO,
      keyRing = FileAccessKeyRing.system)
}

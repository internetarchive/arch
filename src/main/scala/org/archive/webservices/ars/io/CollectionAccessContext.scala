package org.archive.webservices.ars.io

import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.{ArchConf, LocalArchConf}
import org.archive.webservices.sparkling.io.HdfsIO

class CollectionAccessContext(
    val conf: ArchConf with Serializable,
    val useAitHdfsIO: Boolean = false)
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

object CollectionAccessContext {
  def fromLocalArchConf: CollectionAccessContext =
    new CollectionAccessContext(conf = LocalArchConf.instance)
  def fromLocalArchConf(alwaysAitHdfsIO: Boolean) =
    new CollectionAccessContext(conf = LocalArchConf.instance, useAitHdfsIO = alwaysAitHdfsIO)
}

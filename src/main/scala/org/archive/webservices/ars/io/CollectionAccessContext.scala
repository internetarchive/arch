package org.archive.webservices.ars.io

import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.{ArchConf, LocalArchConf}
import org.archive.webservices.sparkling.io.HdfsIO

class CollectionAccessContext(
    val conf: ArchConf with Serializable,
    val alwaysAitHdfsIO: Boolean = false)
    extends Serializable {
  @transient lazy val hdfsIO: HdfsIO = if (alwaysAitHdfsIO) aitHdfsIO else HdfsIO
  @transient lazy val aitHdfsIO: HdfsIO =
    conf.aitCollectionHdfsHostPort
      .map { case (host, port) => HdfsIO(host, port) }
      .getOrElse(HdfsIO)

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
    new CollectionAccessContext(conf = LocalArchConf.instance, alwaysAitHdfsIO = alwaysAitHdfsIO)
}

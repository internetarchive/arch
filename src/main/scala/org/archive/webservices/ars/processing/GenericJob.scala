package org.archive.webservices.ars.processing

trait GenericJob extends DerivationJob {
  override def enqueue(
      conf: DerivationJobConf,
      get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] = {
    super.enqueue(conf, get).flatMap(GenericJobManager.enqueue)
  }
}

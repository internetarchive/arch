package org.archive.webservices.ars.processing

abstract class SparkJob extends DerivationJob {
  override def enqueue(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.enqueue(conf)
    SparkManager.enqueue(instance)
    instance
  }
}

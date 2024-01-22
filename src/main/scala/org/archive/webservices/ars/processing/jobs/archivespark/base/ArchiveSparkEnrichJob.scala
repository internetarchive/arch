package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.processing.DerivationJobConf

import scala.reflect.ClassTag

abstract class ArchiveSparkEnrichJob[Root <: EnrichRoot: ClassTag]
    extends ArchiveSparkBaseJob[Root] {
  def functions(conf: DerivationJobConf): Seq[EnrichFunc[Root, _, _]]
  def filter(rdd: RDD[Root], conf: DerivationJobConf): RDD[Root] = rdd
  override def filterEnrich(rdd: RDD[Root], conf: DerivationJobConf): RDD[Root] = {
    var enriched = filter(rdd, conf)
    for (func <- functions(conf)) enriched = enriched.enrich(func)
    enriched
  }
}

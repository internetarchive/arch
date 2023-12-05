package org.archive.webservices.ars.processing.jobs.archivespark


import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}

import scala.reflect.ClassTag

abstract class ArchiveSparkEnrichJob[Root <: EnrichRoot : ClassTag] extends ArchiveSparkBaseJob[Root] {
  def functions: Seq[EnrichFunc[Root, _, _]]
  def filter(rdd: RDD[Root]): RDD[Root] = rdd
  override def filterEnrich(rdd: RDD[Root]): RDD[Root] = {
    var enriched = filter(rdd)
    for (func <- functions) enriched = enriched.enrich(func)
    enriched
  }
}

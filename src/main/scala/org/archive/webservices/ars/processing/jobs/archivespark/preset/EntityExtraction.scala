package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkArchJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobParameters}

abstract class EntityExtraction extends ArchiveSparkArchJob {
  val name: String = "Named entities"
  val description: String =
    "Names of persons, organizations, and geographic locations detected in each text-bearing document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  override val infoUrl = "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

  val category: ArchJobCategory = ArchJobCategories.Text

  override def filter(rdd: RDD[ArchEnrichRoot[_]], conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = {
    rdd.filter(_.mime.startsWith("text/"))
  }

  def entitiesFunc(params: DerivationJobParameters): EnrichFunc[ArchEnrichRoot[_], _, _] = {
    EntitiesAdapter.withParams(params)
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    Seq(entitiesFunc(conf.params))
  }
}

object EntityExtraction extends EntityExtraction {
  val uuid: String = "018d114d-3426-730e-94a1-b56ca73fc1ad"
}

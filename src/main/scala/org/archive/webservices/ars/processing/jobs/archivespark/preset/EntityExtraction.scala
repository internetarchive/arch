package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobParameters}

abstract class EntityExtraction extends ArchiveSparkEnrichJob {
  override val infoUrl =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

  val category: ArchJobCategory = ArchJobCategories.Text

  override def warcPredicate(conf: DerivationJobConf): ArchWarcRecord => Boolean = {
    val superFilter = super.warcPredicate(conf)
    warc => superFilter(warc) && warc.status == 200
  }

  override def genericPredicate(conf: DerivationJobConf): ArchEnrichRoot[_] => Boolean = {
    record => record.mime.startsWith("text/")
  }

  def entitiesFunc(params: DerivationJobParameters): EnrichFunc[ArchEnrichRoot[_], _, _] = {
    EntitiesAdapter.withParams(params)
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    Seq(entitiesFunc(conf.params))
  }
}

object EntityExtraction extends EntityExtraction {
  val name: String = "Named entities"
  val description: String =
    "Names of persons, organizations, and geographic locations detected in each text-bearing document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  val uuid: String = "018d114d-3426-730e-94a1-b56ca73fc1ad"
}

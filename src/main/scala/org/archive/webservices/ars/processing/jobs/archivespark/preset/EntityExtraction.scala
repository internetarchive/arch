package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobParameters}

abstract class EntityExtraction extends ArchiveSparkEnrichJob {
  val name: String = "Named entities"
  val description: String =
    "Names of persons, organizations, and geographic locations detected in each text-bearing document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  override val infoUrl = "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

  val category: ArchJobCategory = ArchJobCategories.Text

  override def genericPredicate(record: ArchEnrichRoot[_]): Boolean = record.mime.startsWith("text/")
  override def warcPredicate(warc: ArchWarcRecord): Boolean = super.warcPredicate(warc) && warc.status == 200

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

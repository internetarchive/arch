package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter

object EntityExtraction extends ArchiveSparkEnrichJob {
  val name: String = "Named entities"
  val description: String =
    "Names of persons, organizations, and geographic locations detected in each text-bearing document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  val uuid: String = "018d114d-3426-730e-94a1-b56ca73fc1ad"

  override val infoUrl =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

  val category: ArchJobCategory = ArchJobCategories.Text

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    mime => mime.startsWith("text/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    Seq(EntitiesAdapter.withParams(conf.params))
  }

  override val paramsSchema = EntitiesAdapter.paramsSchema
}

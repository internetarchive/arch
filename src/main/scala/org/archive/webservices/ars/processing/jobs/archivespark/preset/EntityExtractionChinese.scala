package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchEnrichRoot

object EntityExtractionChinese extends EntityExtraction {
  val uuid: String = "018d1151-3a3a-7184-b6ed-8ec176ee750e"

  override val name: String = EntityExtraction.name + " (Chinese)"
  override val description: String = EntityExtraction.description + " (Chinese)"

  override def entitiesFunc(
      params: DerivationJobParameters): EnrichFunc[ArchEnrichRoot[_], _, _] = {
    super.entitiesFunc(params.set("lang", "chinese"))
  }
}

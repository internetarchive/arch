package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.TrOCR
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter

object TrOcrEntityExtraction extends AiJob {
  val uuid: String = "019078a8-7b16-7a87-8b50-a30166e547dd"

  val name: String = "Named entities from TrOCR text"
  val description: String =
    "Names of persons, organizations, and geographic locations detected in each TrOCR processed image in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  override val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  override def warcPredicate(conf: DerivationJobConf): ArchWarcRecord => Boolean = {
    val superFilter = super.warcPredicate(conf)
    warc => superFilter(warc) && warc.status == 200
  }

  override def genericPredicate(conf: DerivationJobConf): ArchEnrichRoot[_] => Boolean = {
    record => record.mime.startsWith("image/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    val text = TrOCR.noParams
    val entities = EntitiesAdapter.noParams(on = EntitiesAdapter.toDependencyPointer(text))
    Seq(text, entities)
  }
}

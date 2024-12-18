package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter

object WhisperEntityExtraction extends AiJob {
  val uuid: String = "018f7b09-f7ca-756d-a4ca-69cea914185d"

  val name: String = "Named entities from speech recognition"
  val description: String =
    "Names of persons, organizations, geographic locations, and dates in text transcribed from collection audio and video documents. Output: one or more JSONL files comprising a JSON object for each input record."

  override def infoUrl: String = "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

  override val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  override def warcPredicate(conf: DerivationJobConf): ArchWarcRecord => Boolean = {
    val superFilter = super.warcPredicate(conf)
    warc => superFilter(warc) && warc.status == 200
  }

  override def genericPredicate(conf: DerivationJobConf): ArchEnrichRoot[_] => Boolean = {
    record => record.mime.startsWith("audio/") || record.mime.startsWith("video/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    val whisperText = archivespark.functions.WhisperText.withParams(conf.params)
    val entities = EntitiesAdapter.noParams(on = EntitiesAdapter.toDependencyPointer(whisperText))
    Seq(whisperText, entities)
  }
}

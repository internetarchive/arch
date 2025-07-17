package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter
import org.archive.webservices.sparkling.cdx.CdxRecord

object WhisperEntityExtraction extends AiJob {
  val uuid: String = "018f7b09-f7ca-756d-a4ca-69cea914185d"

  val name: String = "Speech recognition with named entities"
  val description: String =
    "Text, names of persons, organizations, geographic locations, and dates transcribed from collection audio and video documents. Output: one JSONL file comprising a JSON object for each input record. Values include filename and path, MIME type, transcription, persons, organizations, locations, and dates."

  override def infoUrl: String =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

  override def codeUrl: String =
    "https://github.com/internetarchive/arch/blob/main/src/main/scala/org/archive/webservices/ars/processing/jobs/archivespark/preset/WhisperEntityExtraction.scala"

  override val category: ArchJobCategory = ArchJobCategories.Speech

  override def cdxPrefilter(conf: DerivationJobConf): CdxRecord => Boolean = {
    val superFilter = super.cdxPrefilter(conf)
    cdx => superFilter(cdx) && cdx.status == 200
  }

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    mime => mime.startsWith("audio/") || mime.startsWith("video/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    val whisperText = archivespark.functions.WhisperText.withParams(conf.params)
    val entities = EntitiesAdapter.noParams(on = Some(whisperText))
    Seq(whisperText, entities)
  }
}

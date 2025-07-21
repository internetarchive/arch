package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.Whisper
import org.archive.webservices.sparkling.cdx.CdxRecord

object WhisperTranscription extends AiJob {
  val uuid: String = "018f7b0a-4f3c-7846-862a-ff1ae26ce139"

  val name: String = "Speech recognition with technical metadata"
  val description: String =
    "Raw transcription output from each segment of speech recognized in collection audio and video documents with associated technical metadata. Output: one JSONL file comprising a JSON object for each text segment. Values include filename and path, MIME type, text, segment ID, seek, start and end, tokens, temperature, and confidence scores."

  override def infoUrl: String =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/14410760790164-ARCH-Text-datasets#speech"

  override def codeUrl: String =
    "https://github.com/internetarchive/arch/blob/main/src/main/scala/org/archive/webservices/ars/processing/jobs/archivespark/preset/WhisperTranscription.scala"

  override val category: ArchJobCategory = ArchJobCategories.Speech

  override def cdxPrefilter(conf: DerivationJobConf): CdxRecord => Boolean = {
    val superFilter = super.cdxPrefilter(conf)
    cdx => superFilter(cdx) && cdx.status == 200
  }

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    mime => mime.startsWith("audio/") || mime.startsWith("video/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    Seq(Whisper.noParams)
  }
}

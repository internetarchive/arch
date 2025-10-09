package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchEnrichRoot

object WhisperText extends AiJob {
  val uuid: String = "0191e26a-056c-77e2-8fe0-dfba9928b3e2"

  val name: String = "Speech recognition"
  val description: String =
    "Text transcribed from speech recognized in collection audio and video documents. Output: one JSONL file comprising a JSON object for each input record. Values include filename and path, MIME type, and transcription."

  override def infoUrl: String =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/14410760790164-ARCH-Text-datasets#speech"

  override def codeUrl: String =
    "https://github.com/internetarchive/arch/blob/main/src/main/scala/org/archive/webservices/ars/processing/jobs/archivespark/preset/WhisperText.scala"

  override val category: ArchJobCategory = ArchJobCategories.Speech

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    WhisperTranscription.mimePrefilter(conf)
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    Seq(archivespark.functions.WhisperText.withParams(conf.params))
  }
}

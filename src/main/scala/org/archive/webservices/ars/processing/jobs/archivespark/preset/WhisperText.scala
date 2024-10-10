package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord}

object WhisperText extends AiJob {
  val uuid: String = "0191e26a-056c-77e2-8fe0-dfba9928b3e2"

  val name: String = "Whisper transcription text"
  val description: String =
    "Whisper-powered transcript in each audio document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  override val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  override def warcPredicate(conf: DerivationJobConf): ArchWarcRecord => Boolean = {
    val superFilter = super.warcPredicate(conf)
    warc => superFilter(warc) && warc.status == 200
  }

  override def genericPredicate(conf: DerivationJobConf): ArchEnrichRoot[_] => Boolean = {
    record => record.mime.startsWith("audio/") || record.mime.startsWith("video/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    Seq(archivespark.functions.WhisperText.withParams(conf.params))
  }
}

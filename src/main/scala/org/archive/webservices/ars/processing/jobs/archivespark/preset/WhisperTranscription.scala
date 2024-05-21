package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.Whisper

object WhisperTranscription extends ArchiveSparkEnrichJob {
  val uuid: String = "018f7b0a-4f3c-7846-862a-ff1ae26ce139"

  val name: String = "Whisper transcription"
  val description: String =
    "Whisper-powered transcript in each audio document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  override val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  override def genericPredicate(record: ArchEnrichRoot[_]): Boolean = record.mime.startsWith("audio/")
  override def warcPredicate(warc: ArchWarcRecord): Boolean = super.warcPredicate(warc) && warc.status == 200

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    Seq(Whisper.noParams)
  }
}
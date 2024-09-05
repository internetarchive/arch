package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.WhisperText
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter

object WhisperEntityExtraction extends ArchiveSparkEnrichJob {
  val uuid: String = "018f7b09-f7ca-756d-a4ca-69cea914185d"

  val name: String = "Named entities from Whisper transcript"
  val description: String =
    "Names of persons, organizations, and geographic locations detected in each transcribed audio document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  override val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  override def warcPredicate(conf: DerivationJobConf): ArchWarcRecord => Boolean = {
    val superFilter = super.warcPredicate(conf)
    warc => superFilter(warc) && warc.status == 200
  }

  override def genericPredicate(conf: DerivationJobConf): ArchEnrichRoot[_] => Boolean = {
    record => record.mime.startsWith("audio/") || record.mime.startsWith("video/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    val whisperText = WhisperText.noParams
    val entities = EntitiesAdapter.noParams(on = EntitiesAdapter.toDependencyPointer(whisperText))
    Seq(whisperText, entities)
  }
}

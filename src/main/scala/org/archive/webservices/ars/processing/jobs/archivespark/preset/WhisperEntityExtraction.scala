package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.functions.Entities
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkArchJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.WhisperText
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter

object WhisperEntityExtraction extends ArchiveSparkArchJob {
  val uuid: String = "018f7b09-f7ca-756d-a4ca-69cea914185d"

  val name: String = "Named entities from Whisper transcript"
  val description: String =
    "Names of persons, organizations, and geographic locations detected in each transcribed audio document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  override val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  override def filter(rdd: RDD[ArchEnrichRoot[_]], conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = {
    rdd.filter(_.mime.startsWith("audio/"))
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    val whisperText = WhisperText.noParams
    val entities = EntitiesAdapter.noParams(on = EntitiesAdapter.toDependencyPointer(whisperText))
    Seq(whisperText, entities)
  }
}
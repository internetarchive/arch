package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.apache.spark.rdd.RDD
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkArchJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.Whisper

object WhisperTranscription extends ArchiveSparkArchJob {
  val uuid: String = "018f7b0a-4f3c-7846-862a-ff1ae26ce139"

  val name: String = "Whisper transcription"
  val description: String =
    "Whisper-powered transcript in each audio document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."

  override val category: ArchJobCategory = ArchJobCategories.BinaryInformation

  override def filter(rdd: RDD[ArchEnrichRoot[_]], conf: DerivationJobConf): RDD[ArchEnrichRoot[_]] = {
    rdd.filter(_.mime.startsWith("audio/"))
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    Seq(Whisper.noParams)
  }
}
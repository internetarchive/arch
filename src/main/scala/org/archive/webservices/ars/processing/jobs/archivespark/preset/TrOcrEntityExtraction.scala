package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.TrOCR
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter
import org.archive.webservices.sparkling.cdx.CdxRecord

object TrOcrEntityExtraction extends AiJob {
  val uuid: String = "019078a8-7b16-7a87-8b50-a30166e547dd"

  val name: String = "Text recognition with named entities"
  val description: String =
    "Text, names of persons, organizations, geographic locations, and dates transcribed from collection images. Output: one JSONL file comprising a JSON object for each input record. Values include filename and path, MIME type, transcription, persons, organizations, locations, and dates."

  override def infoUrl: String =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

  override def codeUrl: String =
    "https://github.com/internetarchive/arch/blob/main/src/main/scala/org/archive/webservices/ars/processing/jobs/archivespark/preset/TrOcrEntityExtraction.scala"

  override val category: ArchJobCategory = ArchJobCategories.Images

  override def cdxPrefilter(conf: DerivationJobConf): CdxRecord => Boolean = {
    val superFilter = super.cdxPrefilter(conf)
    cdx => superFilter(cdx) && cdx.status == 200
  }

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    _.startsWith("image/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    val text = TrOCR.noParams
    val entities = EntitiesAdapter.noParams(on = EntitiesAdapter.toDependencyPointer(text))
    Seq(text, entities)
  }
}

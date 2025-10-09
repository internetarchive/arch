package jobs

import functions.ExtractedText

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.EntitiesAdapter

object TextExtractionEntities extends ArchiveSparkEnrichJob {
  val name: String = "Named entities from extracted text"
  val description: String = "Names of persons, organizations, geographic locations, and dates in extract text from almost any text document. Output: one or more JSONL files comprising a JSON object for each input record."

  val uuid: String = "0196ce9a-db79-7442-b3a8-9977e65494c8"

  override def infoUrl: String = "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

  override def codeUrl: String =
    "https://github.com/internetarchive/arch/blob/main/job_plugins/text_extraction/jobs/TextExtractionEntities.scala"

  val category: ArchJobCategory = ArchJobCategories.Text

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    TextExtraction.mimePrefilter(conf)
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    val extractedText = ExtractedText.withParams(conf.params)
    val entities = EntitiesAdapter.noParams(on = Some(extractedText))
    Seq(extractedText, entities)
  }
}

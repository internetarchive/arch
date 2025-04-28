package jobs

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.ExtractedText
import org.archive.webservices.sparkling.cdx.CdxRecord

object TextExtraction extends ArchiveSparkEnrichJob {
  val name: String = "Extracted text"
  val description: String =
    "Extract text from almost any text document. Output: one or more JSONL files comprising a JSON object for each input record."

  val uuid: String = "01961974-d4a5-7c2b-822c-b07b68fd66ed"

  val category: ArchJobCategory = ArchJobCategories.Text

  override def cdxPrefilter(conf: DerivationJobConf): CdxRecord => Boolean = {
    val superFilter = super.cdxPrefilter(conf)
    cdx => superFilter(cdx) && cdx.status == 200
  }

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    mime => ExtractedText.SupportedMimes.contains(mime)
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    Seq(ExtractedText.withParams(conf.params))
  }
}
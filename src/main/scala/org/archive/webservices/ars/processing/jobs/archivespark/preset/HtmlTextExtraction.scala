package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.functions.HtmlText
import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.Whisper

object HtmlTextExtraction extends ArchiveSparkEnrichJob {
  val uuid: String = "01994d93-c0c5-7b87-95b1-f01d7c0d940b"

  val name: String = "Plain text extraction from HTML"
  val description: String =
    "Text from a webpage without HTML tags. Output: one JSONL file comprising a JSON object for each text segment. Values include filename and path, MIME type, text, segment ID, seek, start and end, tokens, temperature, and confidence scores."

  override val category: ArchJobCategory = ArchJobCategories.Text

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    mime => mime == "text/html"
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    Seq(HtmlText)
  }
}

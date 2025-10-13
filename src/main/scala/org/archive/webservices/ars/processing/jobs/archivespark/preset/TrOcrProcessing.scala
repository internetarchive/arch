package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchEnrichRoot
import org.archive.webservices.ars.processing.jobs.archivespark.functions.TrOCR

object TrOcrProcessing extends AiJob {
  val uuid: String = "019078a5-c6f3-7051-bb71-5b1f135307df"

  val name: String = "Text recognition"
  val description: String =
    "Text recognized and transcribed from images in a collection, including handwriting. Output: one JSONL file comprising a JSON object for each input record. Values include filename and path, MIME type, and transcription."

  override def infoUrl: String =
    "https://arch-webservices.zendesk.com/hc/en-us/articles/39991661250580-ARCH-Image-datasets#trocr"

  override def codeUrl: String =
    "https://github.com/internetarchive/arch/blob/main/src/main/scala/org/archive/webservices/ars/processing/jobs/archivespark/preset/TrOcrProcessing.scala"

  override val category: ArchJobCategory = ArchJobCategories.Images

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = {
    mime => mime.startsWith("image/") || mime == "application/pdf"
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    Seq(TrOCR.noParams)
  }
}

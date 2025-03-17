package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchWarcRecord}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.TrOCR

object TrOcrProcessing extends AiJob {
  val uuid: String = "019078a5-c6f3-7051-bb71-5b1f135307df"

  val name: String = "Text recognition"
  val description: String =
    "Text recognized and transcribed from images in a collection, including handwriting. Output: one or more JSONL files comprising a JSON object for each input record."

  override def infoUrl: String = "https://arch-webservices.zendesk.com/hc/en-us/articles/14410760790164-ARCH-Text-datasets#ocr"

  override val category: ArchJobCategory = ArchJobCategories.Text

  override def warcPredicate(conf: DerivationJobConf): ArchWarcRecord => Boolean = {
    val superFilter = super.warcPredicate(conf)
    warc => superFilter(warc) && warc.status == 200
  }

  override def genericPredicate(conf: DerivationJobConf): ArchEnrichRoot[_] => Boolean = {
    record => record.mime.startsWith("image/")
  }

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], _, _]] = {
    Seq(TrOCR.noParams)
  }
}

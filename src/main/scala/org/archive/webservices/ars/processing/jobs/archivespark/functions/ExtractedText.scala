package org.archive.webservices.ars.processing.jobs.archivespark.functions

import de.l3s.boilerpipe.extractors.ArticleExtractor
import org.apache.tika.Tika
import org.archive.webservices.archivespark.model.dataloads.ByteLoad
import org.archive.webservices.archivespark.model.pointers.{DataLoadPointer, FieldPointer}
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichable}
import org.archive.webservices.archivespark.specific.warc.functions.WarcPayloadFields
import org.archive.webservices.archivespark.util.Bytes
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchEnrichRoot
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.ArchArchiveSparkFunctionAdapter
import org.archive.webservices.sparkling.html.HtmlProcessor
import org.archive.webservices.sparkling.http.HttpMessage
import org.archive.webservices.sparkling.util.RegexUtil

object ExtractedText extends ArchArchiveSparkFunctionAdapter[Bytes, String] {
  lazy val tika = new Tika()

  override def baseFunc: EnrichFunc[ArchEnrichRoot[_], EnrichRoot, Bytes, String] = new ExtractedText

  override def initFunc(params: DerivationJobParameters): EnrichFunc[_, _, Bytes, String] = {
    val qualityFilter = params.get[Boolean]("quality-filter").getOrElse(false)
    new ExtractedText(qualityFilter)
  }

  val PageMime: String = "text/html"
  val SupportedMimes: Set[String] = Set(
    PageMime, 
    "application/atom+xml",
    "application/vnd.openxmlformats-officedocument.presentationml.slide",
    "application/x-xliff+xml",
    "application/vnd.ms-word.document.macroenabled.12",
    "application/pdf",
    "application/rss+xml",
    "text/html",
    "text/csv",
    "application/vnd.ms-word2006ml",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.oasis.opendocument.text-master",
    "application/vnd.wap.xhtml+xml",
    "application/msword",
    "text/x-groovy",
    "application/vnd.oasis.opendocument.chart-template",
    "application/rtf",
    "application/vnd.oasis.opendocument.text",
    "application/vnd.ms-powerpoint.template.macroenabled.12",
    "application/vnd.ms-word.template.macroenabled.12",
    "application/vnd.oasis.opendocument.text-template",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
    "application/vnd.ms-excel.sheet.macroenabled.12",
    "application/vnd.ms-xpsdocument",
    "application/vnd.ms-visio.template.macroenabled.12",
    "application/vnd.ms-excel.template.macroenabled.12",
    "application/vnd.oasis.opendocument.formula",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint.presentation.macroenabled.12",
    "application/xml",
    "application/xhtml+xml",
    "application/vnd.sun.xml.writer",
    "application/vnd.oasis.opendocument.formula-template",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-visio.stencil.macroenabled.12",
    "text/plain",
    "text/x-c++src",
    "application/vnd.openxmlformats-officedocument.presentationml.template",
    "application/vnd.ms-wordml",
    "application/vnd.oasis.opendocument.graphics-template",
    "application/vnd.oasis.opendocument.flat.presentation",
    "application/vnd.ms-powerpoint.slide.macroenabled.12",
    "application/vnd.oasis.opendocument.text-web",
    "application/vnd.oasis.opendocument.presentation",
    "application/vnd.oasis.opendocument.chart",
    "application/vnd.ms-excel.sheet.binary.macroenabled.12",
    "application/vnd.oasis.opendocument.spreadsheet",
    "application/vnd.oasis.opendocument.image",
    "application/vnd.oasis.opendocument.flat.text",
    "text/tsv",
    "application/vnd.ms-visio.drawing.macroenabled.12",
    "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
  )

  val MinTextLength = 300

  def cleanText(text: String): String = RegexUtil.newLineSpaceTrim(text).trim

  def qualityFilter(text: String): Option[String] = {
    Some(cleanText(text)).filter(_.length > MinTextLength).filter { str =>
      val lineLengths = str.split("\n").map(_.length)
      lineLengths.length > 0 && lineLengths.map(l => if (l < MinTextLength) -l else l).sum > 0
    }
  }
}

class ExtractedText(qualityFilter: Boolean = false) extends EnrichFunc[ArchEnrichRoot[_], EnrichRoot, Bytes, String] {
  override def source: FieldPointer[ArchEnrichRoot[_], Bytes] = DataLoadPointer(ByteLoad)
  override def fields: Seq[String] = Seq("extracted-text")
  override def derive(source: TypedEnrichable[Bytes], derivatives: Derivatives): Unit = {
    val mime = source.root.asInstanceOf[ArchEnrichRoot[_]].mime
    if (ExtractedText.SupportedMimes.contains(mime)) {
      val text = if (mime == ExtractedText.PageMime) {
        val headers = source.field[Seq[(String, String)]](WarcPayloadFields.HttpHeader).toIterator.flatten
        val charset = headers.find {
          _._1.toLowerCase == "charset"
        }.map(_._2).flatMap(HttpMessage.charsetFromContentTypeHeader)
        val bodyText = HtmlProcessor.readStream(source.get.stream, charset.toSeq ++ HttpMessage.BodyCharsets)
        ArticleExtractor.INSTANCE.getText(bodyText)
      } else {
        ExtractedText.tika.parseToString(source.get.stream)
      }
      if (qualityFilter) {
        derivatives <<? ExtractedText.qualityFilter(text)
      } else {
        derivatives << ExtractedText.cleanText(text)
      }
    }
  }
}

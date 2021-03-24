package org.archive.webservices.ars.processing.jobs

import java.io.PrintStream

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.PDFInformationExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.processing.jobs.shared.BinaryInformationAutJob

object PdfInformationExtraction extends BinaryInformationAutJob {
  val name = "Extract PDF information"

  val description =
    "Create a CSV with the following columns: crawl date, URL of the PDF file, filename, PDF extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, PDF MD5 hash and PDF SHA1 hash."

  val targetFile: String = "pdf-information.csv.gz"

  override def printToOutputStream(out: PrintStream): Unit =
    out.println(
      "crawl_date, url, filename, extension, mime_type_web_server, mime_type_tika, width, height, md5, sha1")

  def df(rdd: RDD[ArchiveRecord]) = PDFInformationExtractor(rdd.pdfs())
}

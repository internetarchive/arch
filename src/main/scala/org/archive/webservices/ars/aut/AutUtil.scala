package org.archive.webservices.ars.aut

import java.io.InputStream

import io.archivesunleashed.matchbox.ExtractDomain
import javax.imageio.ImageIO
import org.archive.helge.sparkling.http.HttpMessage
import org.archive.helge.sparkling.warc.WarcRecord

object AutUtil {
  def url(r: WarcRecord): String = r.url.getOrElse("")

  def crawlDate(r: WarcRecord): String =
    r.timestamp.filter(_.length >= 8).map(_.take(8)).getOrElse("")

  def mime(http: HttpMessage): String = http.mime.getOrElse("unknown")

  def checkPageMime(url: String, mime: String): Boolean = {
    val u = url.toLowerCase
    (mime == "text/html" || mime == "application/xhtml+xml" || u.endsWith("htm") || u.endsWith(
      "html")) && !u.endsWith("robots.txt")
  }

  def validPage(r: WarcRecord, http: HttpMessage): Boolean = {
    crawlDate(r).nonEmpty && checkPageMime(url(r), http.mime.getOrElse("")) && http.status == 200
  }

  def extractDomainRemovePrefixWWW(url: String): String = {
    Option(if (url.trim.isEmpty) "" else ExtractDomain(url).replaceAll("^\\s*www\\.", ""))
      .map(_.trim)
      .getOrElse("")
  }

  // see io.archivesunleashed.matchbox.ComputeImageSize
  def computeImageSize(in: InputStream): (Int, Int) = {
    val nullImage = (0, 0)
    try {
      val stream = ImageIO.createImageInputStream(in)
      try {
        val readers = ImageIO.getImageReaders(stream)
        if (readers.hasNext) {
          val reader = readers.next
          reader.setInput(stream)
          (reader.getWidth(0), reader.getHeight(0))
        } else nullImage
      } finally {
        stream.close()
      }
    } catch {
      case e: Throwable => nullImage
    }
  }

  def extractLinks(
      func: (String, String) => Seq[(String, String, String)],
      url: String,
      body: String): Seq[(String, String, String)] = {
    func(url, body).flatMap {
      case (s, t, a) =>
        for {
          source <- Option(s).map(_.trim).filter(_.nonEmpty)
          target <- Option(t).map(_.trim).filter(_.nonEmpty)
        } yield (source, target, Option(a).map(_.trim).getOrElse(""))
    }
  }
}

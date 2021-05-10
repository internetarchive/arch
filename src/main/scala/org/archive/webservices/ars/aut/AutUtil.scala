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
    if (url.trim.isEmpty) "" else ExtractDomain(url).replaceAll("^\\s*www\\.", "")
  }

  // see io.archivesunleashed.matchbox.ComputeImageSize
  def computeImageSize(in: InputStream): (Int, Int) = {
    val nullImage = (0, 0)
    try {
      val image = ImageIO.read(in)
      if (image == null) nullImage else (image.getWidth, image.getHeight)
    } catch {
      case e: Throwable => nullImage
    }
  }
}

package org.archive.webservices.ars.aut

import io.archivesunleashed.matchbox.ExtractDomain
import org.archive.webservices.ars.util.PublicSuffixUtil
import org.archive.webservices.sparkling.http.HttpMessage
import org.archive.webservices.sparkling.warc.WarcRecord

import java.io.InputStream
import javax.imageio.ImageIO

object AutUtil {
  val months = Seq(
    "jan",
    "feb",
    "mar",
    "apr",
    "may",
    "jun",
    "jul",
    "aug",
    "sep",
    "oct",
    "nov",
    "dec").zipWithIndex.map { case (s, d) => (s, ("0" + (d + 1)).takeRight(2)) }

  def url(r: WarcRecord): String = r.url.getOrElse("")

  def timestamp(r: WarcRecord): String =
    r.timestamp.filter(_.length >= 14).map(_.take(14)).getOrElse("")

  def mime(http: HttpMessage): String = http.mime.getOrElse("unknown")

  def checkPageMime(url: String, mime: String): Boolean = {
    val u = url.toLowerCase
    (mime == "text/html" || mime == "application/xhtml+xml" || u.endsWith("htm") || u.endsWith(
      "html")) && !u.endsWith("robots.txt")
  }

  def validPage(r: WarcRecord, http: HttpMessage): Boolean = {
    timestamp(r).nonEmpty && checkPageMime(url(r), http.mime.getOrElse("")) && http.status == 200
  }

  def extractDomainRemovePrefixWWW(url: String, publicSuffixes: Set[String]): String = {
    Option(if (url.trim.isEmpty) "" else ExtractDomain(url).replaceAll("^\\s*www\\.", ""))
      .map(_.trim)
      .map(PublicSuffixUtil.resolve(_, publicSuffixes))
      .getOrElse("")
  }

  def extractDomainRemovePrefixWWW(url: String): String = {
    Option(if (url.trim.isEmpty) "" else ExtractDomain(url).replaceAll("^\\s*www\\.", ""))
      .map(_.trim)
      .getOrElse("")
  }

  def rfc1123toTime14(lastModifiedDate: String): String = {
    if (lastModifiedDate.isEmpty) {
      ""
    } else {
      val lc = lastModifiedDate.toLowerCase
      val date = months.find(m => lc.contains(m._1)).map(_._2).flatMap { m =>
        val d = lc
          .replace(":", "")
          .split(' ')
          .drop(1)
          .map(d => (d.length, d))
          .toMap
        for (y <- d.get(4); n <- d.get(2); t <- d.get(6))
          yield y + m + n + t
      }
      date match {
        case Some(date) =>
          date
        case None =>
          ""
      }
    }
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

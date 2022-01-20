package org.archive.webservices.ars.aut

import java.io.InputStream

import org.apache.tika.Tika
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.parser.AutoDetectParser
import org.archive.webservices.sparkling.io.IOUtil

import scala.util.Try

object TikaUtil {
  val detector = new DefaultDetector()
  val parser = new AutoDetectParser(detector)
  val tika = new Tika(detector, parser)

  def mime(in: InputStream): String = {
    (if (in.markSupported() && IOUtil.eof(in)) None else Try(tika.detect(in)).toOption)
      .getOrElse("N/A")
  }
}

package org.archive.webservices.ars.util

import org.apache.commons.io.input.BoundedInputStream
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.html.HtmlProcessor
import org.archive.webservices.sparkling.http.HttpMessage
import org.archive.webservices.sparkling.io.CatchingInputStream
import org.archive.webservices.sparkling.util.StringUtil

import java.io.InputStream

object HttpUtil {
  val MaxContentLength: Long = 1.mb

  def bodyString(body: InputStream, http: HttpMessage): String = {
    val boundedBody = new BoundedInputStream(body, MaxContentLength)
    HtmlProcessor.readStream(
      new CatchingInputStream(boundedBody),
      http.charset.toSeq ++ HttpMessage.BodyCharsets)
  }
}

package org.archive.webservices.ars.util

import org.apache.commons.io.input.BoundedInputStream
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.html.HtmlProcessor
import org.archive.webservices.sparkling.http.HttpMessage
import org.archive.webservices.sparkling.io.CatchingInputStream

import java.io.InputStream
import java.net.{HttpURLConnection, InetSocketAddress, Proxy, URL}
import javax.net.ssl.HttpsURLConnection

object HttpUtil {
  val MaxContentLength: Long = 1.mb

  def bodyString(body: InputStream, http: HttpMessage): String = {
    val boundedBody = new BoundedInputStream(body, MaxContentLength)
    HtmlProcessor.readStream(
      new CatchingInputStream(boundedBody),
      http.charset.toSeq ++ HttpMessage.BodyCharsets)
  }

  lazy val proxy: Proxy = {
    val split = ArchConf.httpProxy.split(':')
    if (split.length > 1) {
      new Proxy(Proxy.Type.HTTP, new InetSocketAddress(split.head, split(1).toInt))
    } else Proxy.NO_PROXY
  }

  def openConnection(url: String): HttpURLConnection = {
    val u = new URL(url)
    if (ArchConf.httpProxyHosts.contains(u.getHost)) {
      u.openConnection(proxy)
    } else u.openConnection()
  }.asInstanceOf[HttpURLConnection]
}

package org.archive.webservices.ars.io

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.matchbox.ExtractDomain
import org.archive.helge.sparkling.http.HttpMessage
import org.archive.helge.sparkling.io.{ByteArray, IOUtil}
import org.archive.helge.sparkling.util.StringUtil
import org.archive.helge.sparkling.warc.{WarcHeaders, WarcRecord}

object AutRecordLoader {
  def fromWarc(filename: String, warc: WarcRecord, bufferBytes: Boolean = false): ArchiveRecord =
    new ArchiveRecord {
      private var bufferedPayload: Option[ByteArray] = None

      private var invalidated: Boolean = false
      private def payload: InputStream = {
        if (bufferBytes) {
          if (bufferedPayload.isEmpty)
            bufferedPayload = Some(MemoryCompressor.compress(warc.payload))
          MemoryCompressor.decompress(bufferedPayload.get)
        } else {
          if (invalidated) throw new RuntimeException("Payload invalidated.")
          invalidated = true
          warc.payload
        }
      }

      private var httpInvalidated: Boolean = false

      lazy val http: Option[HttpMessage] = {
        val http = HttpMessage.get(payload)
        if (http.isEmpty) invalidated = false // HttpMessage.get resets payload
        http
      }

      private def nestedHttp[A](get: HttpMessage => A): Option[A] = {
        if (http.isDefined) {
          if (!httpInvalidated) {
            httpInvalidated = true
            http.map(get)
          } else HttpMessage.get(payload).map(get)
        } else None
      }

      def nestedPayload: InputStream = nestedHttp(_.body).getOrElse(payload)

      def getArchiveFilename: String = filename
      def getCrawlDate: String = warc.timestamp.filter(_.length >= 8).map(_.take(8)).getOrElse("")
      def getCrawlMonth: String =
        warc.timestamp.filter(_.length >= 6).map(_.take(6)).getOrElse("")

      def getContentBytes: Array[Byte] = {
        nestedHttp { http =>
          val bytes = new ByteArrayOutputStream()
          bytes.write(WarcHeaders.http(http.statusLine, http.headers))
          IOUtil.copy(http.body, bytes)
          bytes.flush()
          bytes.toByteArray
        }.getOrElse(IOUtil.bytes(payload))
      }

      def getContentString: String = {
        nestedHttp[String] { http =>
          (Seq(http.statusLine) ++
            http.headers.map { case (k, v) => k + ": " + v } ++
            Seq("") ++
            Seq(http.bodyString)).mkString(WarcHeaders.Br)
        }.getOrElse(StringUtil.fromBytes(getContentBytes))
      }

      def getMimeType: String = http.flatMap(_.mime).getOrElse("unknown")

      def getUrl: String = warc.url.getOrElse("")
      def getDomain: String = ExtractDomain(getUrl)

      def getBinaryBytes: Array[Byte] = IOUtil.bytes(nestedPayload)

      def getHttpStatus: String = http.map(_.status).map(_.toString).getOrElse("000")

      def getPayloadDigest: String = WarcRecord.defaultDigestHash(nestedPayload)
    }
}

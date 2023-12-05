package org.archive.webservices.ars.model.collections.filespecs

import io.circe.HCursor
import org.archive.webservices.ars.io.CollectionAccessContext

import java.io.{FileNotFoundException, InputStream}
import java.net.URL
import scala.io.Source
import scala.util.Try

class S3FileRecordFactory(location: String, longestPrefixMapping: Boolean) extends FileRecordFactory[Map[String, Any]] {
  class S3FileRecord private[S3FileRecordFactory] (val filename: String, val mime: String, meta: Map[String, Any]) extends FileRecord {
    override lazy val path: String = locateFile(filename)
    override def access: InputStream = accessFile(filename, resolve = false)
    override def meta[A](key: String): Option[A] = meta.get(key).flatMap(v => Try(v.asInstanceOf[A]).toOption)
  }

  override def get(filename: String, mime: String, meta: Map[String, Any]): FileRecord = new S3FileRecord(filename, mime, meta)

  def accessFile(filename: String, resolve: Boolean = true, accessContext: CollectionAccessContext): InputStream = {
    val url = if (resolve) locateFile(filename) + "/" + filename else filename
    println(s"Reading $url...")
    new URL(url).openStream
  }

  private val prefixes = collection.mutable.Map.empty[String, Set[String]]
  def locateFile(filename: String): String = {
    if (longestPrefixMapping) {
      var remaining = filename
      var prefix = ""
      var next = nextPrefixes(prefix)
      while (next.nonEmpty) {
        val keys = next.map(p => (p, p.stripPrefix(prefix).stripSuffix("/"))).filter(_._2.nonEmpty)
        val longest = keys.filter { case (_, k) =>
          remaining.startsWith(k)
        }.toSeq.sortBy(-_._2.length).headOption.orElse {
          keys.filter { case (_, k) =>
            filename.startsWith(k)
          }.toSeq.sortBy(-_._2.length).headOption
        }
        if (longest.isEmpty) throw new FileNotFoundException(filename + s" ($prefix)")
        val (p, k) = longest.get
        if (k == filename) return location + "/" + prefix.stripSuffix("/")
        if (remaining.startsWith(k)) remaining = remaining.stripPrefix(k)
        prefix = p
        next = nextPrefixes(prefix)
      }
      throw new FileNotFoundException(filename + s" ($prefix)")
    } else location
  }

  def nextPrefixes(prefix: String): Set[String] = prefixes.getOrElseUpdate(prefix, {
    val url = location + "?delimiter=/&prefix=" + prefix
    val source = Source.fromURL(url)
    try {
      source.mkString.split('<').filter(keyValue => keyValue.startsWith("Prefix>") || keyValue.startsWith("Key>")).map { keyValue =>
        keyValue.split('>').last
      }.toSet
    } finally {
      source.close()
    }
  })
}

object S3FileRecordFactory {
  def apply(spec: HCursor): S3FileRecordFactory = {
    spec.get[String]("remote-location").toOption.map { location =>
      val longestPrefixMapping = spec.get[String]("remote-path-mapping").toOption.contains("longest-prefix")
      new S3FileRecordFactory(location, longestPrefixMapping)
    }.getOrElse {
      throw new RuntimeException("No location URL specified.")
    }
  }
}
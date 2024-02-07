package org.archive.webservices.ars.model.collections.inputspecs

import scala.io.Source

class S3HttpFileRecordFactory(location: String, longestPrefixMapping: Boolean)
    extends HttpFileRecordFactory(location) with LongestPrefixProbing {
  override def locateFile(filename: String): String = {
    if (longestPrefixMapping) location + "/" + locateLongestPrefix(filename)
    else super.locateFile(filename)
  }

  private val prefixes = collection.mutable.Map.empty[String, Set[String]]
  override protected def nextPrefixes(prefix: String): Set[String] = {
    prefixes.getOrElseUpdate(prefix, {
      val url = location + "?delimiter=/&prefix=" + prefix
      val source = Source.fromURL(url)
      try {
        source.mkString
          .split('<')
          .filter(keyValue => keyValue.startsWith("Prefix>") || keyValue.startsWith("Key>"))
          .map { keyValue =>
            keyValue.split('>').last
          }
          .toSet
      } finally {
        source.close()
      }
    })
  }
}

object S3HttpFileRecordFactory {
  def apply(spec: InputSpec): S3HttpFileRecordFactory = {
    spec
      .str("data-location")
      .map { location =>
        val longestPrefixMapping = spec.str("data-path-mapping").contains("longest-prefix")
        new S3HttpFileRecordFactory(location, longestPrefixMapping)
      }
      .getOrElse {
        throw new RuntimeException("No location URL specified.")
      }
  }
}

package org.archive.webservices.ars.model.collections.inputspecs

import scala.io.Source

class S3HttpFileRecordFactory(location: String, longestPrefixMapping: Boolean)
    extends HttpFileRecordFactory(location)
    with LongestPrefixProbing {
  override def companion: FileFactoryCompanion = S3HttpFileRecordFactory

  override def locateFile(file: String): String = {
    if (longestPrefixMapping) FileRecordFactory.filePath(locateLongestPrefixPath(file), file)
    else super.locateFile(file)
  }

  private val prefixes = collection.mutable.Map.empty[String, Set[String]]
  override protected def nextPrefixes(prefix: String): Set[String] = {
    prefixes.getOrElseUpdate(
      prefix, {
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

object S3HttpFileRecordFactory extends FileFactoryCompanion {
  val dataSourceType: String = "s3-http"

  def apply(spec: InputSpec): S3HttpFileRecordFactory = {
    spec
      .str(InputSpec.DataLocationKey)
      .map { location =>
        val longestPrefixMapping = spec.str("dataPathMapping").contains("longest-prefix")
        new S3HttpFileRecordFactory(location, longestPrefixMapping)
      }
      .getOrElse {
        throw new RuntimeException("No location URL specified.")
      }
  }
}

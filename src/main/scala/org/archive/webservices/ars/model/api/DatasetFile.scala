package org.archive.webservices.ars.model.api

import org.archive.webservices.ars.model.DerivativeOutput
import org.archive.webservices.ars.util.FormatUtil

import java.time.Instant

case class DatasetFile(
    filename: String,
    sizeBytes: Long,
    mimeType: String,
    lineCount: Long,
    fileType: String,
    creationTime: String,
    md5Checksum: Option[String],
    accessToken: String)
    extends ApiResponseObject[DatasetFile]

object DatasetFile {
  def apply(derivOut: DerivativeOutput): DatasetFile =
    DatasetFile(
      filename = derivOut.filename,
      sizeBytes = derivOut.size,
      lineCount = derivOut.lineCount,
      mimeType = derivOut.mimeType,
      fileType = derivOut.fileType,
      creationTime = FormatUtil.instantTimeString(Instant.ofEpochMilli(derivOut.time)),
      md5Checksum = derivOut.checksums.get("md5"),
      accessToken = derivOut.accessToken)
}

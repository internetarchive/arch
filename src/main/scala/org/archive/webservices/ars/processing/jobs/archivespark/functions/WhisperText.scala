package org.archive.webservices.ars.processing.jobs.archivespark.functions

import io.circe.Json
import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot, GlobalEnrichFunc}
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.ArchArchiveSparkFunction

object WhisperText
    extends GlobalEnrichFunc[EnrichRoot with LocalFileCache, Json, String]
    with ArchArchiveSparkFunction[Json] {
  val func: EnrichFunc[EnrichRoot with LocalFileCache, Json, String] = Whisper.map("text") {
    json =>
      json.asArray.toSeq.flatten.flatMap(_.hcursor.get[String]("text").toOption).mkString
  }
}

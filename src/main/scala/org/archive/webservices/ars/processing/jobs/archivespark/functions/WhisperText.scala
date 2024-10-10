package org.archive.webservices.ars.processing.jobs.archivespark.functions

import io.circe.Json
import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot, GlobalEnrichFunc}
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.ArchArchiveSparkFunctionAdapter

object WhisperText extends ArchArchiveSparkFunctionAdapter[Json] {
  override def initFunc(params: DerivationJobParameters): EnrichFunc[_, Json, _] = {
    params.get[Double]("maxNoSpeechProb") match {
      case Some(maxNoSpeechProb) => new WhisperText(maxNoSpeechProb)
      case None => super.initFunc(params)
    }
  }

  override def baseFunc: EnrichFunc[_, Json, _] = new WhisperText(0.5)
}

class WhisperText(maxNoSpeechProb: Double)
    extends GlobalEnrichFunc[EnrichRoot with LocalFileCache, Json, String] {
  val func: EnrichFunc[EnrichRoot with LocalFileCache, Json, String] = Whisper.func.map("text") {
    json =>
      json.asArray.toSeq.flatten
        .map(_.hcursor)
        .filter { cursor =>
          cursor.get[Double]("no_speech_prob").exists(_ <= maxNoSpeechProb)
        }
        .flatMap { cursor =>
          cursor.get[String]("text").toOption
        }
        .mkString
  }
}

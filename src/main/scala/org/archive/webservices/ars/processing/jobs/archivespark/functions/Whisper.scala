package org.archive.webservices.ars.processing.jobs.archivespark.functions

import _root_.io.circe.parser._
import io.circe.Json
import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.CondaBasedArchiveSparkFunctionAdapter

object Whisper extends CondaBasedArchiveSparkFunctionAdapter[Json] {
  override def func: CondaBasedFunction[Json] = new Whisper
}

class Whisper extends CondaBasedFunction[Json] {
  override val label: String = "whisper"
  override val dataDir: String = s"$label/20240807195100"
  override val condaEnv: String = s"conda-$label-env"
  override val pythonFile: String = s"$label-run.py"
  override val pythonArgumentFiles: Seq[String] = Seq("base.en.pt")

  override def processOutput(output: String): Option[Json] = {
    val trim = output.trim
    if (trim.isEmpty) None
    else
      parse(trim) match {
        case Left(failure) =>
          Arch.reportError(
            s"ArchiveSpark Whisper Output JSON Parsing Error",
            failure.getMessage(),
            Map("output" -> output))
          None
        case Right(json) => Some(json)
      }
  }
}

package org.archive.webservices.ars.processing.jobs.archivespark.functions
import _root_.io.circe.parser._
import io.circe.Json

object Whisper extends CondaBasedFunction[Json] {
  override val dataDir: String = "whisper"
  override val condaEnv: String = "conda-whisper-env"
  override val pythonFile: String = "whisper-run.py"
  override val pythonArgumentFiles: Seq[String] = Seq("base.en.pt")

  override def processOutput(output: String): Option[Json] = parse(output.trim).toOption
}

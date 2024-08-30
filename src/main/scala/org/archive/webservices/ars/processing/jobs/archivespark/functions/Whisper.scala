package org.archive.webservices.ars.processing.jobs.archivespark.functions
import _root_.io.circe.parser._
import io.circe.Json

object Whisper extends CondaBasedFunction[Json] {
  override val label: String = "whisper"
  override val dataDir: String = s"$label/20240807195100"
  override val condaEnv: String = s"conda-$label-env"
  override val pythonFile: String = s"$label-run.py"
  override val pythonArgumentFiles: Seq[String] = Seq("base.en.pt")

  override def processOutput(output: String): Option[Json] = {
    val trim = output.trim
    if (trim.isEmpty) None else parse(trim).toOption
  }
}

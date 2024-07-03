package org.archive.webservices.ars.processing.jobs.archivespark.functions
import _root_.io.circe.parser._
import io.circe.Json
import org.archive.webservices.archivespark.model.Derivatives
import org.archive.webservices.ars.io.SystemProcess
import org.archive.webservices.ars.model.ArchConf

import java.io.File

object Whisper extends CondaBasedFunction[Json] {
  override val hdfsDataDir: String = "whisper"
  override val condaEnv: String = "conda-whisper-env"
  override val pythonFile: String = "whisper-run.py"
  override val pythonArgumentFiles: Seq[String] = Seq("base.en.pt")

  override def processOutput(output: String): Option[Json] = parse(output.trim).toOption
}

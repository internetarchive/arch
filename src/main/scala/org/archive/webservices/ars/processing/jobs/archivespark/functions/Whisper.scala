package org.archive.webservices.ars.processing.jobs.archivespark.functions
import _root_.io.circe.parser._
import io.circe.Json
import org.archive.webservices.archivespark.model.Derivatives
import org.archive.webservices.ars.io.SystemProcess

object Whisper extends ArchFileProcEnrichFuncBase[Json] {
  val HdfsDir = "/user/helge/arch-data/whisper"
  val PythonFile = "whisper-run.py"
  val CondaFile = "conda-whisper-env.tar.gz"
  val CondaEnv = "conda-whisper-env"
  val ModelFile = "base.en.pt"
  val OutputEndToken = "##"

  override val workingDir: String = "/arch-tmp/arch-tmp-whisper-1"

  override def fields: Seq[String] = Seq("whisper")

  override def init(): Option[SystemProcess] = {
    make(CondaEnv) { dir =>
      make(CondaFile) { _ =>
        copyFromHdfs(HdfsDir, CondaFile)
      }
      dir.mkdir()
      bash.exec(s"tar -xzf $CondaFile -C $CondaEnv")
    }

    bash.exec(s"source $CondaEnv/bin/activate")

    make(PythonFile) { _ =>
      copyFromHdfs(HdfsDir, PythonFile)
    }

    make(ModelFile) { _ =>
      copyFromHdfs(HdfsDir, ModelFile)
    }

    println(s"### starting python $PythonFile $ModelFile")
    Some(bash.subProcess(s"python $PythonFile $ModelFile", waitForLine = Some(OutputEndToken)))
  }

  override def cmd(file: String): String = file

  override def process(proc: SystemProcess, derivatives: Derivatives): Unit = {
    println(s"### processing line")
    val output = proc.readToLine(OutputEndToken, includeEnd = false).mkString
    println("### OUTPUT: " + output)
    for (json <- parse(output.trim).toOption) {
      println("#### PARSED, continuing...")
      derivatives << json
    }
    println(s"### processing line: done.")
  }
}

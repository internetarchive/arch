package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.Derivatives
import org.archive.webservices.ars.io.SystemProcess
import org.archive.webservices.ars.model.ArchConf

import java.io.File

abstract class CondaBasedFunction[A] extends ArchFileProcEnrichFuncBase[A] {
  val AdditionalPackagesUnpackedExtension = ".unpacked"

  def hdfsDataDir: String
  def condaEnv: String
  def pythonFile: String
  def additionalPackages: Seq[String] = Seq.empty
  def pythonArgumentFiles: Seq[String]

  val condaFile: String = condaEnv + ".tar.gz"
  val outputEndToken = "##"

  val HdfsDir: String = ArchConf.hdfsDataPath + "/" + hdfsDataDir

  override val workingDir: String = ArchConf.hadoopNodeLocalTempPath + "/" + hdfsDataDir

  override def fields: Seq[String] = Seq("whisper")

  override def init(): Option[SystemProcess] = {
    make(condaEnv) { dir =>
      make(condaFile) { _ =>
        copyFromHdfs(HdfsDir, condaFile)
      }
      dir.mkdir()
      bash.exec(s"tar -xzf $condaFile -C $condaEnv")
      new File(condaFile).delete()
    }

    bash.exec(s"source $condaEnv/bin/activate")

    for (p <- additionalPackages) {
      val unpackedFlag = p + AdditionalPackagesUnpackedExtension
      make(unpackedFlag) { flag =>
        copyFromHdfs(HdfsDir, p)
        try {
          bash.exec(s"tar -xzf $condaFile")
          flag.createNewFile()
        } finally {
          new File(p).delete()
        }
      }
    }

    make(pythonFile) { _ =>
      copyFromHdfs(HdfsDir, pythonFile)
    }

    for (f <- pythonArgumentFiles) {
      make(f) { _ =>
        copyFromHdfs(HdfsDir, f)
      }
    }

    val arg = pythonArgumentFiles.mkString(" ")
    Some(bash.subProcess(s"python $pythonFile $arg", waitForLine = Some(outputEndToken)))
  }

  override def cmd(file: String): String = file

  override def process(proc: SystemProcess, derivatives: Derivatives): Unit = {
    val output = proc.readToLine(outputEndToken, includeEnd = false).mkString
    for (a <- processOutput(output)) derivatives << a
  }

  def processOutput(output: String): Option[A]
}

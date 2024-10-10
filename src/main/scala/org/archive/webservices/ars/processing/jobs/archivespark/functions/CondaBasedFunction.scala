package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.Derivatives
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.io.SystemProcess
import org.archive.webservices.sparkling.logging.{Log, LogContext}

import java.io.File

abstract class CondaBasedFunction[A] extends ArchFileProcEnrichFuncBase[A] {
  implicit private val logContext: LogContext = LogContext(this)

  val AdditionalPackagesUnpackedExtension = "._unpacked"

  override def sharedGlobalProcess: Boolean = true

  def dataDir: String
  def condaEnv: String
  def pythonFile: String
  def additionalPackages: Seq[String] = Seq.empty
  def pythonArgumentFiles: Seq[String]

  private var _additionalPythonArguments = Seq.empty[String]
  def additionalPythonArguments(params: DerivationJobParameters): Seq[String] = Seq.empty

  def initFunc(params: DerivationJobParameters): Unit = {
    _additionalPythonArguments = additionalPythonArguments(params)
  }

  def condaFile: String = condaEnv + ".tar.gz"
  val outputEndToken = "##"

  lazy val hdfsDir: Option[String] = ArchConf.hdfsJobArtifactPath.map(_ + "/" + dataDir)
  lazy val artifactUrl: String = ArchConf.jobArtifactUrl.stripSuffix("/") + "/" + dataDir

  def ensureFile(file: String): File = make(file) { _ =>
    copyFile(file)
  }

  def copyFile(file: String): String = {
    hdfsDir match {
      case Some(dir) => copyFromHdfs(dir, file)
      case None => copyFromUrl(s"$artifactUrl/$file", file)
    }
  }

  override lazy val workingDir: String = ArchConf.hadoopNodeLocalTempPath + "/" + dataDir

  def label: String

  override def fields: Seq[String] = Seq(label)

  override def init(): Option[SystemProcess] = Some {
    val arg = {
      (pythonArgumentFiles.mkString(" ") + " " + _additionalPythonArguments.mkString(" ")).trim
    }
    exec(
      s"python $pythonFile $arg",
      { (shell, cmd) =>
        val condaActivate = s"$condaEnv/bin/activate"
        make(condaActivate) { _ =>
          val f = ensureFile(condaFile)
          make(condaEnv) { dir =>
            dir.mkdir()
          }
          shell.exec(s"tar -xzf $condaFile -C $condaEnv", blocking = true)
          f.delete()
        }

        shell.exec(s"source $condaActivate")

        for (p <- additionalPackages) {
          val unpackedFlag = p + AdditionalPackagesUnpackedExtension
          make(unpackedFlag) { flag =>
            ensureFile(p)
            try {
              shell.exec(s"tar -xzf $p", blocking = true)
              flag.createNewFile()
            } finally {
              new File(p).delete()
            }
          }
        }

        ensureFile(pythonFile)

        for (f <- pythonArgumentFiles) ensureFile(f)

        shell.exec(cmd, waitForLine = Some(outputEndToken), supportsEcho = false)
      })
  }

  override def onError(error: Seq[String]): Unit = {
    Log.error(error.mkString("\n"))
  }

  override def cmd(file: String): String = file

  override def process(proc: SystemProcess, derivatives: Derivatives): Unit = {
    val output =
      proc.readToLine(outputEndToken, includeEnd = false, keepMaxBytes = 1.mb.toInt).mkString
    for (a <- processOutput(output)) derivatives << a
  }

  def processOutput(output: String): Option[A]
}

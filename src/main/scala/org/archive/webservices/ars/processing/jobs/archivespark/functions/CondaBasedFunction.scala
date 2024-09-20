package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc}
import org.archive.webservices.ars.io.SystemProcess
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.sparkling.logging.{Log, LogContext}

import java.io.File

abstract class CondaBasedFunction[A] extends ArchFileProcEnrichFuncBase[A] {
  implicit private val logContext: LogContext = LogContext(this)

  val AdditionalPackagesUnpackedExtension = ".unpacked"

  override def sharedGlobalProcess: Boolean = true

  def dataDir: String
  def condaEnv: String
  def pythonFile: String
  def additionalPackages: Seq[String] = Seq.empty
  def pythonArgumentFiles: Seq[String]

  private var _additionalPythonArguments = Seq.empty[String]
  def additionalPythonArguments(params: DerivationJobParameters): Seq[String] = Seq.empty

  override def initFunc(params: DerivationJobParameters): EnrichFunc[_, String, _] = {
    _additionalPythonArguments = additionalPythonArguments(params)
    super.initFunc(params)
  }

  def condaFile: String = condaEnv + ".tar.gz"
  val outputEndToken = "##"

  lazy val hdfsDir: Option[String] = ArchConf.hdfsJobArtifactPath.map(_ + "/" + dataDir)
  lazy val artifactUrl: String = ArchConf.jobArtifactUrl.stripSuffix("/") + "/" + dataDir

  def copyFile(file: String): String = {
    hdfsDir match {
      case Some(dir) => copyFromHdfs(dir, file)
      case None => copyFromUrl(s"$artifactUrl/$file", file)
    }
  }

  override lazy val workingDir: String = ArchConf.hadoopNodeLocalTempPath + "/" + dataDir

  def label: String

  override def fields: Seq[String] = Seq(label)

  override def init(): Option[SystemProcess] = {
    val condaFile = this.condaFile

    make(condaEnv) { dir =>
      val f = make(condaFile) { _ =>
        copyFile(condaFile)
      }
      dir.mkdir()
      bash.exec(s"tar -xzf $condaFile -C $condaEnv")
      f.delete()
    }

    bash.exec(s"source $condaEnv/bin/activate")

    for (p <- additionalPackages) {
      val unpackedFlag = p + AdditionalPackagesUnpackedExtension
      make(unpackedFlag) { flag =>
        copyFile(p)
        try {
          bash.exec(s"tar -xzf $condaFile")
          flag.createNewFile()
        } finally {
          new File(p).delete()
        }
      }
    }

    make(pythonFile) { _ =>
      copyFile(pythonFile)
    }

    for (f <- pythonArgumentFiles) {
      make(f) { _ =>
        copyFile(f)
      }
    }

    val arg =
      (pythonArgumentFiles.mkString(" ") + " " + _additionalPythonArguments.mkString(" ")).trim
    Some(
      bash.subProcess(
        s"python $pythonFile $arg",
        waitForLine = Some(outputEndToken),
        onError = onError))
  }

  def onError(error: Seq[String]): Unit = {
    throw new RuntimeException(error.mkString("\n"))
  }

  override def cmd(file: String): String = file

  override def process(proc: SystemProcess, derivatives: Derivatives): Unit = {
    val output = proc.readToLine(outputEndToken, includeEnd = false).mkString
    Log.info(s"Output read...")
    for (a <- processOutput(output)) {
      Log.info(s"Output written...")
      derivatives << a
    }
  }

  def processOutput(output: String): Option[A]
}

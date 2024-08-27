package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichable}
import org.archive.webservices.ars.io.SystemProcess
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.ArchArchiveSparkFunction
import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil}

import java.io.File
import java.net.URL

abstract class ArchFileProcEnrichFuncBase[A]
    extends EnrichFunc[EnrichRoot with LocalFileCache, String, A]
    with ArchArchiveSparkFunction[String] {
  val source: FieldPointer[EnrichRoot with LocalFileCache, String] = ArchFileCache

  def suppresInOutSh(script: String): String = {
    Seq("{", script, "} < /dev/null 1>&2").mkString("\n")
  }

  def workingDir: String = ArchConf.hadoopNodeLocalTempPath

  lazy val bash: SystemProcess = {
    val process = SystemProcess.bash
    process.exec(s"cd $workingDir")
    process
  }

  def make(file: String)(make: File => Unit): String = {
    val f = new File(workingDir, file)
    if (f.exists()) return f.getPath
    make(f)
    f.getPath
  }

  def copyFromHdfs(hdfsDir: String, file: String): String = {
    val in = HdfsIO.open(s"$hdfsDir/$file", decompress = false)
    try {
      val localFile = workingDir + "/" + file
      val out = IOUtil.fileOut(localFile)
      try {
        IOUtil.copy(in, out)
      } finally out.close()
      localFile
    } finally in.close()
  }

  def copyFromUrl(url: String, file: String): String = {
    val in = new URL(s"$url/$file").openStream
    try {
      val localFile = workingDir + "/" + file
      val out = IOUtil.fileOut(localFile)
      try {
        IOUtil.copy(in, out)
      } finally out.close()
      localFile
    } finally in.close()
  }

  def cmd(procFile: String): String

  def script(file: String): Option[String] = None

  def init(): Option[SystemProcess] = None

  @transient private var globalProcess: Option[SystemProcess] = None
  @transient private var initialized = false
  private def doInit(): Unit = if (!initialized) synchronized {
    if (!initialized) {
      try {
        initialized = true
        new File(workingDir).mkdirs()
        globalProcess = init()
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }

  def process(process: SystemProcess, derivatives: Derivatives): Unit

  override def derive(source: TypedEnrichable[String], derivatives: Derivatives): Unit = {
    doInit()
    val inFile = source.get
    val file = script(inFile)
      .map { script =>
        val scriptFile = IOUtil.tmpFile
        val scriptOut = IOUtil.print(IOUtil.fileOut(scriptFile))
        try {
          scriptOut.print(script)
        } finally scriptOut.close()
        scriptFile.getPath
      }
      .getOrElse(inFile)
    val proc = globalProcess match {
      case Some(proc) =>
        proc.exec(cmd(file))
        proc
      case None => SystemProcess.exec(cmd(file))
    }
    process(proc, derivatives)
  }
}

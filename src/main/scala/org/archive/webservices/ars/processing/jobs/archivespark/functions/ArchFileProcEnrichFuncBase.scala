package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichable}
import org.archive.webservices.ars.io.SystemProcess
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.ArchArchiveSparkFunction
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil}
import org.archive.webservices.sparkling.logging.{Log, LogContext}

import java.io.{File, PrintWriter, StringWriter}
import java.net.URL

abstract class ArchFileProcEnrichFuncBase[A]
    extends EnrichFunc[EnrichRoot with LocalFileCache, String, A]
    with ArchArchiveSparkFunction[String] {
  implicit private val logContext: LogContext = LogContext(this)

  val source: FieldPointer[EnrichRoot with LocalFileCache, String] = ArchFileCache

  def sharedGlobalProcess: Boolean = false

  def suppresInOutSh(script: String): String = {
    Seq("{", script, "} < /dev/null 1>&2").mkString("\n")
  }

  def workingDir: String = ArchConf.hadoopNodeLocalTempPath

  lazy val bash: SystemProcess = {
    val process = SystemProcess.bash
    process.exec(s"cd $workingDir")
    process
  }

  def make(file: String)(make: File => Unit): File = {
    val f = new File(workingDir, file)
    if (f.exists()) return f
    make(f)
    f
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
    val in = new URL(url).openStream
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

  private val globalProcessKey = "globalProcess"
  private val initializedKey = "initialized"
  private var _sharedGlobalProcess: Option[SystemProcess] = None
  private var _sharedInitialized: Boolean = false

  private def globalProcess: Option[SystemProcess] = {
    if (sharedGlobalProcess) _sharedGlobalProcess
    else Sparkling.taskStore.get(globalProcessKey).map(_.asInstanceOf[SystemProcess])
  }

  private def initialized: Boolean = {
    if (sharedGlobalProcess) _sharedInitialized
    else Sparkling.taskStore.get(initializedKey).exists(_.asInstanceOf[Boolean])
  }

  private def doInit(): Unit = if (!initialized) synchronized {
    if (!initialized) {
      try {
        Log.info(s"Creating working directory: $workingDir...")
        new File(workingDir).mkdirs()
        if (sharedGlobalProcess) {
          _sharedGlobalProcess = init()
          _sharedInitialized = true
        } else {
          for (process <- init()) Sparkling.taskStore.update(globalProcessKey, process)
          Sparkling.taskStore.update(initializedKey, true)
        }
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
    globalProcess match {
      case Some(proc) =>
        if (sharedGlobalProcess) synchronized {
          Log.info(s"Processing file $file...")
          proc.exec(cmd(file))
          process(proc, derivatives)
        } else {
          Log.info(s"Processing file $file...")
          proc.exec(cmd(file))
          process(proc, derivatives)
        }
      case None =>
        Log.info(s"Processing file $file...")
        process(SystemProcess.exec(cmd(file)), derivatives)
    }
  }
}

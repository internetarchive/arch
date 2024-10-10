package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichable}
import org.archive.webservices.ars.io.SystemProcess
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.ars.util.StageSyncManager
import org.archive.webservices.sparkling.util.Common
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil}
import org.archive.webservices.sparkling.logging.{Log, LogContext}

import java.io.{File, InputStream}
import java.net.URL

abstract class ArchFileProcEnrichFuncBase[A]
    extends EnrichFunc[EnrichRoot with LocalFileCache, String, A] {
  implicit private val logContext: LogContext = LogContext(this)

  val source: FieldPointer[EnrichRoot with LocalFileCache, String] = ArchFileCache

  def sharedGlobalProcess: Boolean = false

  def suppresInOutSh(script: String): String = {
    Seq("{", script, "} < /dev/null 1>&2").mkString("\n")
  }

  def workingDir: String = ArchConf.hadoopNodeLocalTempPath

  def bash: SystemProcess = {
    val process = SystemProcess.bash
    process.exec(s"cd $workingDir", onError = Some(onError))
    process
  }

  def onError(error: Seq[String]): Unit = {}

  def make(file: String)(make: File => Unit): File = {
    val f = new File(workingDir, file)
    if (f.exists()) return f
    make(f)
    f
  }

  def copyFile(in: InputStream, file: String): String = {
    try {
      val localFile = workingDir + "/" + file
      val tmpFile = localFile + "._downloading"
      val out = IOUtil.fileOut(tmpFile)
      try {
        IOUtil.copy(in, out)
      } finally out.close()
      val f = new File(localFile)
      if (f.exists()) f.delete()
      new File(tmpFile).renameTo(f)
      localFile
    } finally in.close()
  }

  def copyFromHdfs(hdfsDir: String, file: String): String = {
    copyFile(HdfsIO.open(s"$hdfsDir/$file", decompress = false), file)
  }

  def copyFromUrl(url: String, file: String): String = {
    copyFile(new URL(url).openStream, file)
  }

  def cmd(procFile: String): String

  def script(file: String): Option[String] = None

  def init(): Option[SystemProcess] = None

  private val globalProcessKey = "globalProcess"
  private val initializedKey = "initialized"
  private var _sharedGlobalProcess: Option[SystemProcess] = None

  private def globalProcess: Option[SystemProcess] = {
    if (sharedGlobalProcess) _sharedGlobalProcess.filter(!_.destroyed)
    else Sparkling.taskStore.get(globalProcessKey).map(_.asInstanceOf[SystemProcess])
  }

  private def initialized: Boolean = {
    if (sharedGlobalProcess) _sharedGlobalProcess.exists(!_.destroyed)
    else Sparkling.taskStore.get(initializedKey).exists(_.asInstanceOf[Boolean])
  }

  private def doInit(): Unit = if (!initialized) synchronized {
    if (!initialized) {
      val dir = new File(workingDir)
      dir.mkdirs()
      Common.sync(new File(dir, Sparkling.appId + "._initializing")) {
        if (sharedGlobalProcess) {
          _sharedGlobalProcess = init()
        } else {
          for (process <- init()) Sparkling.taskStore.update(globalProcessKey, process)
          Sparkling.taskStore.update(initializedKey, true)
        }
      }
    }
  }

  def exec(cmd: String, exec: (SystemProcess, String) => Unit): SystemProcess = {
    if (sharedGlobalProcess) {
      StageSyncManager.syncProcess(cmd, workingDir, bash, exec)
    } else {
      val p = bash
      exec(bash, cmd)
      p
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
    Log.info(s"Processing file $file...")
    globalProcess match {
      case Some(proc) =>
        if (sharedGlobalProcess) {
          StageSyncManager.claimProcess(workingDir, proc)
          proc.synchronized {
            proc.exec(cmd(file))
            process(proc, derivatives)
          }
        }
        else proc.synchronized {
          proc.exec(cmd(file))
          process(proc, derivatives)
        }
      case None =>
        process(SystemProcess.exec(cmd(file)), derivatives)
    }
  }
}

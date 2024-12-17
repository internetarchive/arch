package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichable}
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil, StageSyncManager, SystemProcess}
import org.archive.webservices.sparkling.logging.{Log, LogContext}
import org.archive.webservices.sparkling.util.Common

import java.io.{File, InputStream}
import java.net.URL

abstract class ArchFileProcEnrichFuncBase[A]
    extends EnrichFunc[EnrichRoot with LocalFileCache, String, A] {
  implicit private val logContext: LogContext = LogContext(this)

  val source: FieldPointer[EnrichRoot with LocalFileCache, String] = ArchFileCache

  def workingDir: String = ArchConf.hadoopNodeLocalTempPath

  def pidPipe(pid: Int): String = workingDir + "/" + pid + ".pipe"

  def bash: SystemProcess = {
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

  def init(): SystemProcess

  private var _process: Option[SystemProcess] = None
  private def initialized: Boolean = _process.exists(!_.destroyed)

  private def doInit(): Unit = if (!initialized) synchronized {
    if (!initialized) {
      val dir = new File(workingDir)
      dir.mkdirs()
      Common.sync(new File(dir, Sparkling.appId + "._initializing")) {
        _process = Some(init())
      }
    }
  }

  def exec(cmd: String, exec: (SystemProcess, Int, String) => Unit): SystemProcess = {
    StageSyncManager
      .syncProcess(
        cmd,
        workingDir,
        bash,
        exec,
        cleanup = { (p, pid) =>
          val pipe = new File(pidPipe(pid))
          if (pipe.exists()) pipe.delete()
        })
      ._1
  }

  def process(process: SystemProcess, pid: Int, derivatives: Derivatives): Unit

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
    val (proc, pid) = StageSyncManager.claimProcess(workingDir)
    proc.synchronized {
      proc.exec(cmd(file))
      process(proc, pid, derivatives)
    }
  }
}

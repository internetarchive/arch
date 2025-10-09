package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.archivespark.model.{Derivatives, EnrichFunc, EnrichRoot, TypedEnrichable}
import org.archive.webservices.ars.processing.jobs.archivespark.base.LocalFileCache
import org.archive.webservices.sparkling.io.{IOUtil, StageSyncManager, SystemProcess}
import org.archive.webservices.sparkling.logging.{Log, LogContext}
import org.archive.webservices.sparkling.util.Common

import java.io.File

abstract class ArchFileProcEnrichFuncBase[A]
    extends EnrichFunc[EnrichRoot with LocalFileCache, EnrichRoot with LocalFileCache, String, A] {
  implicit private val logContext: LogContext = LogContext(this)

  final val source: FieldPointer[EnrichRoot with LocalFileCache, String] = ArchFileCache

  def workingDir: String

  def bash: SystemProcess = {
    val process = SystemProcess.bash
    process.exec(s"cd $workingDir")
    process
  }

  def cmd(procFile: String): String

  def script(file: String): Option[String] = None

  def init(restart: Boolean = false): SystemProcess

  private var _process: Option[SystemProcess] = None
  private def initialized: Boolean = _process.exists(!_.destroyed)

  private def doInit(restart: Boolean = false): Unit = {
    if (restart) for (p <- _process) p.destroy()
    if (!initialized) synchronized {
      if (!initialized) {
        val dir = new File(workingDir)
        if (!dir.exists) dir.mkdirs()
        _process = Some(init(restart))
      }
    }
  }

  def cleanup(pid: Int): Unit = {}

  def exec(cmd: String, exec: (SystemProcess, Int, String) => Unit, restart: Boolean = false): SystemProcess = {
    StageSyncManager
      .syncProcess(
        cmd,
        workingDir,
        bash,
        exec,
        cleanup = { (_, pid) => cleanup(pid) },
        restart)
      ._1
  }

  def process(process: SystemProcess, pid: Int, derivatives: Derivatives): Unit

  def waitForProc(proc: SystemProcess, pid: Int): Unit = {}

  override def derive(source: TypedEnrichable[String], derivatives: Derivatives): Unit = {
    doInit()
    var firstAttempt = true
    var retry = false
    while (firstAttempt || retry) {
      firstAttempt = false
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
      if (retry) Log.info(s"Retrying $file...")
      Log.info(s"Processing file $file...")
      val (proc, pid, reclaimed) = StageSyncManager.claimProcess(workingDir)
      Log.info(s"Process claimed: $workingDir (reclaimed: $reclaimed), synchronizing...")
      val input = cmd(file)
      proc.lock {
        if (!proc.destroyed) {
          try {
            Common.timeout(600000) { // 10 minutes
              if (reclaimed || retry) {
                Log.info(s"Synchronized, waiting for process $pid...")
                waitForProc(proc, pid)
              }
              Log.info(s"Processing input $input...")
              proc.exec(input)
              Log.info(s"Processed input, processing output...")
              process(proc, pid, derivatives)
              retry = false
              Log.info(s"Done $input.")
            }
          } catch {
            case e: Exception =>
              Log.info(s"Restarting process $workingDir...")
              doInit(true)
              retry = !retry
              if (!retry) throw e
          }
        } else {
          retry = true
        }
      }
    }
  }
}

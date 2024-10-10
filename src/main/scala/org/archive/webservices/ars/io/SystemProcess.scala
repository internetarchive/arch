package org.archive.webservices.ars.io

import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.logging.{Log, LogContext}
import org.archive.webservices.sparkling.util.{IteratorUtil, StringUtil}

import java.io.{BufferedInputStream, BufferedReader, InputStreamReader, PrintStream}
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class SystemProcess private (
    val process: Process,
    private var _supportsEcho: Boolean,
    val in: BufferedInputStream,
    val out: PrintStream,
    private var _onError: Seq[String] => Unit = _ => {}) {
  private var _destroyed = false

  private var lastError = Seq.empty[String]

  private val errorFuture: Future[Boolean] = Future {
    val error = new BufferedReader(new InputStreamReader(process.getErrorStream))
    var line = error.readLine()
    while (line != null) {
      errorFuture.synchronized(lastError :+= line)
      line = error.readLine()
    }
    true
  }

  def destroyed: Boolean = _destroyed

  def destroy(): Unit = synchronized {
    process.destroy()
    _destroyed = true
  }

  def supportsEcho: Boolean = _supportsEcho

  def consumeAllInput(supportsEcho: Boolean = _supportsEcho): Unit = if (supportsEcho) synchronized {
    val endLine = s"${SystemProcess.CommandEndToken} ${Instant.now.toEpochMilli}"
    out.println(s"echo " + endLine)
    consumeToLine(endLine)
  }

  def readAllInput(supportsEcho: Boolean = _supportsEcho): Iterator[String] = if (supportsEcho) synchronized {
    val endLine = s"${SystemProcess.CommandEndToken} ${Instant.now.toEpochMilli}"
    out.println(s"echo " + endLine)
    readToLine(endLine, includeEnd = false)
  } else Iterator.empty

  def readToLine(
      endLine: String,
      prefix: Boolean = false,
      includeEnd: Boolean = true,
      keepMaxBytes: Int = -1): Iterator[String] = synchronized {
    var stop = false
    var remaining = keepMaxBytes
    IteratorUtil.whileDefined {
      if (!stop) {
        val lineFuture = Future {
          val length = if (keepMaxBytes < 0) -1 else if (remaining < 0) 0 else remaining
          StringUtil.readLine(in, maxLength = length)
        }
        while (!lineFuture.isCompleted || lastError.nonEmpty) {
          if (lastError.nonEmpty) errorFuture.synchronized {
            _onError(lastError)
            lastError = Seq.empty
          }
          Thread.`yield`()
        }
        val line = lineFuture.value.get.get
        if (line == null) None
        else {
          stop = if (prefix) line.startsWith(endLine) else line == endLine
          if (!stop || includeEnd) Some {
            if (keepMaxBytes < 0) Some(line)
            else {
              if (remaining > 0) {
                remaining -= line.length
                Some(line)
              } else None
            }
          } else None
        }
      } else None
    }.flatten
  }

  def consumeToLine(endLine: String, prefix: Boolean = false): Unit = synchronized {
    IteratorUtil.consume(readToLine(endLine, prefix))
  }

  def exec(
      cmd: String,
      clearInput: Boolean = true,
      blocking: Boolean = false,
      supportsEcho: Boolean = _supportsEcho,
      waitForLine: Option[String] = None,
      waitForPrefix: Boolean = false,
      onError: Option[Seq[String] => Unit] = None): Unit = synchronized {
    if (clearInput) consumeAllInput()
    for (func <- onError) {
      val currentOnError = _onError
      _onError = error => {
        currentOnError(error)
        func(error)
      }
    }
    out.println(cmd)
    _supportsEcho = supportsEcho
    if (blocking) consumeAllInput()
    for (waitLine <- waitForLine) consumeToLine(waitLine, waitForPrefix)
  }
}

object SystemProcess {
  val CommandEndToken = "END"

  def apply(process: Process, supportsEcho: Boolean = false): SystemProcess = {
    val in = new BufferedInputStream(process.getInputStream)
    val out = IOUtil.print(process.getOutputStream, autoFlush = true)
    new SystemProcess(process, supportsEcho, in, out)
  }

  def bash: SystemProcess = exec("/bin/bash", supportsEcho = true)

  def exec(cmd: String, supportsEcho: Boolean = false): SystemProcess =
    apply(Runtime.getRuntime.exec(cmd), supportsEcho)
}

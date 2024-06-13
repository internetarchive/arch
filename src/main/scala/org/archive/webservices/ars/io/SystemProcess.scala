package org.archive.webservices.ars.io

import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.util.IteratorUtil

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class SystemProcess private (
    val process: Process,
    val supportsEcho: Boolean,
    val in: BufferedReader,
    val out: PrintStream) {

  val errorFuture = Future {
    val error = new BufferedReader(new InputStreamReader(process.getErrorStream))
    var line = error.readLine()
    while (line != null) {
      println("EEE " + line)
      line = error.readLine()
    }
    true
  }

  def consumeAllInput(): Unit = if (supportsEcho) {
    val rnd = Random.nextString(10)
    val endLine = s"${SystemProcess.CommandEndToken} $rnd"
    out.println(s"echo " + endLine)
    consumeToLine(endLine)
  }

  def readToLine(
      endLine: String,
      prefix: Boolean = false,
      includeEnd: Boolean = true): Iterator[String] = {
    var stop = false
    IteratorUtil.whileDefined {
      if (!stop) {
        val line = in.readLine()
        if (line == null) None
        else {
          stop = if (prefix) line.startsWith(endLine) else line == endLine
          if (!stop || includeEnd) Some(line) else None
        }
      } else None
    }
  }

  def consumeToLine(endLine: String, prefix: Boolean = false): Unit = {
    IteratorUtil.consume(readToLine(endLine, prefix))
  }

  def exec(cmd: String, clearInput: Boolean = true): Unit = {
    if (clearInput) consumeAllInput()
    out.println(cmd)
  }

  def subProcess(
      cmd: String,
      clearInput: Boolean = true,
      supportsEcho: Boolean = false,
      waitForLine: Option[String],
      waitForPrefix: Boolean = false): SystemProcess = {
    exec(cmd, clearInput)
    for (waitLine <- waitForLine) {
      println("#### WAITING FOR " + waitLine)
      consumeToLine(waitLine, waitForPrefix)
      println("#### NO LONGER WAITING FOR " + waitLine)
    }
    new SystemProcess(process, supportsEcho, in, out)
  }
}

object SystemProcess {
  val CommandEndToken = "END"

  def apply(process: Process, supportsEcho: Boolean = false): SystemProcess = {
    val in = new BufferedReader(new InputStreamReader(process.getInputStream))
    val out = IOUtil.print(process.getOutputStream, autoFlush = true)
    new SystemProcess(process, supportsEcho, in, out)
  }

  def bash: SystemProcess = exec("/bin/bash", supportsEcho = true)

  def exec(cmd: String, supportsEcho: Boolean = false): SystemProcess =
    SystemProcess(Runtime.getRuntime.exec(cmd), supportsEcho)
}

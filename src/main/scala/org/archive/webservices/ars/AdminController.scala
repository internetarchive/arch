package org.archive.webservices.ars

import org.apache.commons.io.input.BoundedInputStream
import org.archive.webservices.ars.processing.JobStateManager
import org.archive.webservices.ars.processing.JobStateManager.Charset
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.io.IOUtil
import org.scalatra._

import java.io.{File, FileInputStream}
import scala.io.Source
import scala.util.Try

class AdminController extends BaseController {
  val MaxLogLength: Int = 1.mb.toInt
  get("/logs/:log_type") {
    ensureAuth { user =>
      if (user.isAdmin) {
        params("log_type") match {
          case "jobs" =>
            val tail = params.get("tail").flatMap(str => Try(str.toInt).toOption).getOrElse(-1)
            val logFile = new File(s"${JobStateManager.LoggingDir}/${JobStateManager.JobLogFile}")
            val log = if (logFile.exists) {
              val skip = if (tail < 0) 0L else (logFile.length - tail.min(MaxLogLength)).max(0L)
              val in = new FileInputStream(logFile)
              try {
                IOUtil.skip(in, skip)
                val source = Source.fromInputStream(
                  new BoundedInputStream(in, MaxLogLength),
                  JobStateManager.Charset)
                try {
                  source.mkString
                } finally {
                  source.close()
                }
              } finally {
                in.close()
              }
            } else ""
            Ok(log, Map("Content-Type" -> "text/plain"))
          case "running" =>
            val runningJobsFile =
              new File(s"${JobStateManager.LoggingDir}/${JobStateManager.RunningJobsFile}")
            val log = if (runningJobsFile.exists) {
              val source = Source.fromFile(runningJobsFile, Charset)
              try {
                source.mkString
              } finally {
                source.close()
              }
            } else ""
            Ok(log, Map("Content-Type" -> "application/json"))
          case "failed" =>
            val failedJobsFile =
              new File(s"${JobStateManager.LoggingDir}/${JobStateManager.FailedJobsFile}")
            val log = if (failedJobsFile.exists) {
              val source = Source.fromFile(failedJobsFile, Charset)
              try {
                source.mkString
              } finally {
                source.close()
              }
            } else ""
            Ok(log, Map("Content-Type" -> "text/plain"))
          case _ =>
            NotFound()
        }
      } else Forbidden()
    }
  }
}

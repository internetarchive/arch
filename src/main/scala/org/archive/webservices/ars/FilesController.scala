package org.archive.webservices.ars

import javax.servlet.http.HttpServletRequest
import org.archive.webservices.ars.model.DerivativeOutput
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.archive.webservices.sparkling.io.HdfsIO
import org.scalatra.{ActionResult, Forbidden, NotFound, Ok, ResponseStatus}

import scala.util.Try

class FilesController extends BaseController {
  private def sendFile(file: DerivativeOutput)(implicit request: HttpServletRequest): ActionResult = {
    val size = file.size
    val rangeStrOpt = request.header("Range").map(_.trim).filter(_.startsWith("bytes=")).map(_.stripPrefix("bytes=").split(',').head.trim).filter(_.nonEmpty)
    val range = rangeStrOpt.map { rangeStr =>
      if (rangeStr.startsWith("-")) {
        val len = Try(rangeStr.stripPrefix("-").toLong).toOption.getOrElse(0L)
        (size - len, size - 1)
      } else {
        val split = rangeStr.split('-')
        val from = split.headOption.flatMap(str => Try(str.toLong).toOption).getOrElse(0L)
        val to = split.drop(1).headOption.flatMap(str => Try(str.toLong).toOption).getOrElse(0L)
        (from.max(0L), to.min(size - 1))
      }
    }.filter{case (from, to) => from >= 0 && to < size && to >= from && (to - from + 1) < size}
    val (offset, length, status) = range.map{case (from, to) => (from, to - from  + 1, 206)}.getOrElse((0L, size, 200))
    ActionResult(ResponseStatus(status),
      HdfsIO.open(
        file.path,
        offset = offset,
        length = length,
        decompress = false,
        strategy = HdfsIO.LoadingStrategy.Remote),
      Map(
        "Content-Type" -> file.mimeType,
        "Accept-Ranges" -> "bytes",
        "Content-Disposition" -> ("attachment; filename=" + file.filename),
        "Content-Length" -> length.toString) ++ range.map { case (from, to) =>
        "Content-Range" -> s"bytes $from-$to/$size"
      })
  }

  get("/download/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val sample = params.get("sample").contains("true")
    val filename = params("file_name")
    params.get("access") match {
      case Some(accessToken) =>
        val jobId = params("job_id")
        (for {
          conf <- DerivationJobConf.collection(collectionId, sample = sample)
          instance <- JobManager.getInstance(jobId, conf)
        } yield {
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) =>
              if (file.accessToken == accessToken) {
                sendFile(file)
              } else Forbidden()
            case None =>
              NotFound()
          }
        }).getOrElse(NotFound())
      case None =>
        ensureLogin(redirect = false, useSession = true, validateCollection = Some(collectionId)) {
          implicit user =>
            val jobId = params("job_id")
            (for {
              conf <- DerivationJobConf.collection(collectionId, sample = sample)
              instance <- JobManager.getInstance(jobId, conf)
            } yield {
              instance.outFiles.find(_.filename == filename) match {
                case Some(file) =>
                  sendFile(file)
                case None =>
                  NotFound()
              }
            }).getOrElse(NotFound())
        }
    }
  }

  get("/preview/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val sample = params.get("sample").contains("true")
    val filename = params("file_name")
    ensureLogin(redirect = false, useSession = true, validateCollection = Some(collectionId)) {
      implicit user =>
        val jobId = params("job_id")
        (for {
          conf <- DerivationJobConf.collection(collectionId, sample = sample)
          instance <- JobManager.getInstance(jobId, conf)
        } yield {
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) =>
              Ok(
                HdfsIO.lines(file.path, n = 51).mkString("\n"),
                Map("Content-Type" -> file.mimeType))
            case None =>
              NotFound()
          }
        }).getOrElse(NotFound())
    }
  }
}

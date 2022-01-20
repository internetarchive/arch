package org.archive.webservices.ars

import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.archive.webservices.sparkling.io.HdfsIO
import org.scalatra.{Forbidden, NotFound, Ok}

class FilesController extends BaseController {
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
                Ok(
                  HdfsIO.open(
                    file.path,
                    decompress = false,
                    strategy = HdfsIO.LoadingStrategy.Remote),
                  Map(
                    "Content-Type" -> file.mimeType,
                    "Content-Disposition" -> ("attachment; filename=" + file.filename)))
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
                  Ok(
                    HdfsIO.open(file.path, decompress = false),
                    Map(
                      "Content-Type" -> file.mimeType,
                      "Content-Disposition" -> ("attachment; filename=" + file.filename)))
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

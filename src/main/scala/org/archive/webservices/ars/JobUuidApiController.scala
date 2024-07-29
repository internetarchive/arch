package org.archive.webservices.ars

import _root_.io.circe.parser.parse
import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.{ArchConf, PublishedDatasets}
import org.archive.webservices.ars.model.api.DatasetFile
import org.archive.webservices.ars.processing.{DerivationJobInstance, JobManager, SampleVizData}
import org.scalatra._

class JobUuidApiController extends BaseController {
  val UuidParam = "uuid"
  val UuidPattern = s"/:$UuidParam/"

  def response(action: DerivationJobInstance => ActionResult): ActionResult = {
    ensureAuth { implicit context =>
      val uuid = params(UuidParam)
      JobManager.getInstance(uuid) match {
        case Some(instance) =>
          if (context.isAdmin || instance.user.contains(context.user)) {
            action(instance)
          } else Forbidden()
        case None =>
          NotFound()
      }
    }
  }

  get(UuidPattern + "state") {
    response(ApiController.jobStateResponse)
  }

  get(UuidPattern + "result") {
    response { instance =>
      WasapiController.files(
        instance,
        params.get("base_download_url").getOrElse(
          ArchConf.baseUrl + "/api/job/" + params(UuidParam) + "/download"
        ),
        params,
        addSample = false)
    }
  }

  get(UuidPattern + "files") {
    response { instance =>
      Ok(
        // Temporarily skip retrieving files for WAT/WANE and ArchiveSpark* job types
        // until peformance issue is resolved, see: WT-2870
        if (
          instance.job == org.archive.webservices.ars.processing.jobs.ArsWatGeneration
            || instance.job == org.archive.webservices.ars.processing.jobs.ArsWaneGeneration
            || instance.job == org.archive.webservices.ars.processing.jobs.archivespark.ArchiveSparkEntityExtraction
            || instance.job == org.archive.webservices.ars.processing.jobs.archivespark.ArchiveSparkEntityExtractionChinese
        )
          Seq.empty.asInstanceOf[Seq[_root_.io.circe.Json]].asJson
        else
          instance.outFiles.map(DatasetFile.apply).map(_.toJson).toArray.asJson,
        Map("Content-Type" -> "application/json"))
    }
  }

  get(UuidPattern + "download/:file") {
    val filename = params("file")
    params.get("access") match {
      case Some(accessToken) =>
        val uuid = params(UuidParam)
        JobManager.getInstance(uuid) match {
          case Some(instance) =>
            instance.outFiles.find(_.filename == filename) match {
              case Some(file) =>
                if (file.accessToken == accessToken) {
                  FilesController.sendFile(file.prefixDownload(instance))
                } else Forbidden()
              case None =>
                NotFound()
            }
          case None =>
            NotFound()
        }
      case None =>
        response { instance =>
          val filename = params("file")
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) => FilesController.sendFile(file.prefixDownload(instance))
            case None => NotFound()
          }
        }
    }
  }

  get(UuidPattern + "preview/:file") {
    response { instance =>
      FilesController.preview(instance, params("file"))
    }
  }

  get(UuidPattern + "sample_viz_data") {
    response { instance =>
      instance.sampleVizData match {
        case Some(data: SampleVizData) =>
          Ok(data.asJson, Map("Content-Type" -> "application/json"))
        case _ => NotFound()
      }
    }
  }

  get(UuidPattern + "colab/:file") {
    params.get("access") match {
      case Some(accessToken) =>
        response { instance =>
          val filename = params("file")
          val fileUrl = params
            .get("file_download_url")
            .getOrElse(
              ArchConf.baseUrl + "/api/job/" + params(UuidParam) + "/download/" + filename)
          FilesController.colab(instance, filename, fileUrl, accessToken)
        }
      case None => Forbidden()
    }
  }

  get(UuidPattern + "petabox/metadata") {
    response { instance =>
      PublishedDatasets
        .metadata(instance)
        .map { metadata =>
          Ok(metadata.asJson.spaces4, Map("Content-Type" -> "application/json"))
        }
        .getOrElse(NotFound())
    }
  }

  post(UuidPattern + "petabox/metadata") {
    response { instance =>
      parse(request.body).toOption
        .map(PublishedDatasets.parseJsonMetadata)
        .map { metadata =>
          PublishedDatasets.validateMetadata(metadata) match {
            case Some(error) => BadRequest(error)
            case None =>
              if (PublishedDatasets.updateItem(instance, metadata)) {
                Ok("Success.")
              } else InternalServerError("Updating metadata failed.")
          }
        }
        .getOrElse(BadRequest("Invalid metadata JSON."))
    }
  }

  post(UuidPattern + "petabox/delete") {
    val doDelete = parse(request.body).toOption
      .flatMap(_.hcursor.get[Boolean]("delete").toOption)
      .getOrElse(false)
    if (doDelete) {
      response { instance =>
        if (PublishedDatasets.deletePublished(instance)) Ok("Success.")
        else InternalServerError("Deleting item failed.")
      }
    } else {
      BadRequest(
        "In order to confirm the deletion, please send a JSON with boolean key 'delete' set to true.")
    }
  }

  get(UuidPattern + "published") {
    response { instance =>
      PublishedDatasets
        .jobItem(instance)
        .map { info =>
          Ok(info.toJson(includeItem = true).spaces4, Map("Content-Type" -> "application/json"))
        }
        .getOrElse(NotFound())
    }
  }
}

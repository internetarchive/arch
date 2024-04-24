package org.archive.webservices.ars

import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.{DerivationJobInstance, JobManager}
import org.scalatra.{ActionResult, Forbidden, NotFound}

class JobUuidApiController extends BaseController {
  val UuidParam = "uuid"
  val UuidPattern = s"/:$UuidParam/"

  def response(action: DerivationJobInstance => ActionResult): ActionResult = {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
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

  get(UuidPattern + "files") {
    response { instance =>
      WasapiController.files(
        instance,
        ArchConf.baseUrl + "/api/job/" + params(UuidParam) + "/download",
        params,
        addSample = false)
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

  get(UuidPattern + "colab/:file") {
    params.get("access") match {
      case Some(accessToken) =>
        response { instance =>
          val filename = params("file")
          val fileUrl =
            ArchConf.baseUrl + "/api/job/" + params(UuidParam) + "/download/" + filename
          FilesController.colab(instance, filename, fileUrl, accessToken)
        }
      case None => Forbidden()
    }
  }
}

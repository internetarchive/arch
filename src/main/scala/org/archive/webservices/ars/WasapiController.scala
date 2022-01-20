package org.archive.webservices.ars

import _root_.io.circe._
import _root_.io.circe.syntax._
import org.apache.http.client.utils.URIBuilder
import org.archive.webservices.ars.model.DerivativeOutput
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.scalatra.{NotFound, Ok}

import scala.collection.immutable.ListMap
import scala.util.Try

object WasapiController {
  val FixedPageSize = 100
}

class WasapiController extends BaseController {
  // implementing WASAPI's result endpoint (see https://github.com/WASAPI-Community/data-transfer-apis/tree/master/ait-specification#checking-the-result-of-a-complete-job)
  get("/v1/jobs/:jobid/result") {
    params.get("collection") match {
      case Some(collectionId) =>
        ensureLogin(redirect = false, useSession = true, validateCollection = Some(collectionId)) {
          user =>
            val jobId = params("jobid")
            DerivationJobConf
              .collection(collectionId, params.get("sample").contains("true"))
              .flatMap(JobManager.getInstance(jobId, _)) match {
              case Some(instance) =>
                val page = params
                  .get("page")
                  .flatMap(p => Try(p.toInt).toOption)
                  .filter(_ >= 1)
                  .getOrElse(1)
                var files = Seq.empty[DerivativeOutput]
                val count = instance.outFiles
                  .grouped(WasapiController.FixedPageSize)
                  .zipWithIndex
                  .map {
                    case (pageItems, idx) =>
                      if (idx == page - 1) files = pageItems
                      pageItems.toIterator.size
                  }
                  .sum
                val pages = (count.toDouble / WasapiController.FixedPageSize).ceil.toInt
                var uriBuilder =
                  new URIBuilder(request.uri).setParameter("collection", collectionId)
                if (instance.conf.isSample) uriBuilder = uriBuilder.setParameter("sample", "true")
                Ok(
                  ListMap(
                    "count" -> count.asJson,
                    "next" -> (if (page < pages)
                                 uriBuilder
                                   .setParameter("page", (page + 1).toString)
                                   .build()
                                   .toString
                                   .asJson
                               else Json.Null),
                    "previous" -> (if (page > 1)
                                     uriBuilder
                                       .setParameter("page", (page - 1).min(pages).toString)
                                       .build()
                                       .toString
                                       .asJson
                                   else Json.Null),
                    "files" -> files.map { file =>
                      val locationUrl = Arch.BaseUrl + "/files/download/" + collectionId + "/" + instance.job.id + "/" + file.filename + (if (instance.conf.isSample)
                                                                                                                                            "?sample=true&access="
                                                                                                                                          else
                                                                                                                                            "?access=") + file.accessToken
                      ListMap(
                        "collection" -> collectionId.asJson,
                        "filename" -> file.filename.asJson,
                        "filetype" -> file.fileType.asJson,
                        "locations" -> Seq(locationUrl).asJson,
                        "size" -> file.size.asJson).asJson
                    }.asJson).asJson.spaces4,
                  Map("Content-Type" -> "application/json"))
              case None =>
                NotFound()
            }
        }
      case None =>
        NotFound("collection parameter missing")
    }
  }
}

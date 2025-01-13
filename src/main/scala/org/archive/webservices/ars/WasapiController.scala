package org.archive.webservices.ars

import org.apache.http.client.utils.URIBuilder
import org.archive.webservices.ars.model.api.{WasapiResponse, WasapiResponseFile}
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.model.{ArchCollection, ArchConf, DerivativeOutput, DerivativeOutputCache}
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobInstance, JobManager}
import org.archive.webservices.ars.util.LazyCache
import org.scalatra.{ActionResult, NotFound}

import javax.servlet.http.HttpServletRequest
import scala.util.Try

object WasapiController {
  val FixedPageSize = 100

  def files(
      instance: DerivationJobInstance,
      baseDownloadUrl: String,
      page: Int,
      addSample: Boolean = true)(implicit request: HttpServletRequest): ActionResult = {
    LazyCache.lazyJsonResponse[DerivativeOutputCache, (Int, Seq[DerivativeOutput])](
      instance.lazyOutFilesCache,
      { cache =>
        (cache.count, cache.files.drop((page - 1) * FixedPageSize).take(FixedPageSize).toSeq)
      }, {
        var files = Seq.empty[DerivativeOutput]
        val count = instance.outFiles
          .grouped(WasapiController.FixedPageSize)
          .zipWithIndex
          .map { case (pageItems, idx) =>
            if (idx == page - 1) files = pageItems
            pageItems.toIterator.size
          }
          .sum
        (count, files)
      },
      { case (count, files) =>
        val pages = (count.toDouble / WasapiController.FixedPageSize).ceil.toInt
        var uriBuilder = new URIBuilder(request.getRequestURI)
        if (InputSpec.isCollectionBased(instance.conf.inputSpec)) {
          uriBuilder = uriBuilder.setParameter("collection", instance.conf.inputSpec.collectionId)
        }
        if (addSample && instance.conf.isSample)
          uriBuilder = uriBuilder.setParameter("sample", "true")
        WasapiResponse(
          count = count,
          next =
            (if (page < pages)
               Some(uriBuilder.setParameter("page", (page + 1).toString).build().toString)
             else None),
          previous =
            (if (page > 1)
               Some(
                 uriBuilder.setParameter("page", (page - 1).min(pages).toString).build().toString)
             else None),
          files = files.map { file =>
            val locationUriBuilder = new URIBuilder(s"${baseDownloadUrl}/${file.filename}")
            if (addSample && instance.conf.isSample)
              locationUriBuilder.setParameter("sample", "true")
            locationUriBuilder.setParameter("access", file.accessToken)
            WasapiResponseFile(
              filename = file.filename,
              filetype = file.fileType,
              checksums = file.checksums,
              locations = Seq(locationUriBuilder.build.toString),
              size = file.size,
              collection =
                if (InputSpec.isCollectionBased(instance.conf.inputSpec))
                  Some(instance.conf.inputSpec.collectionId)
                else None)
          }).toJson
      })
  }
}

class WasapiController extends BaseController {
  // implementing WASAPI's result endpoint (see https://github.com/WASAPI-Community/data-transfer-apis/tree/master/ait-specification#checking-the-result-of-a-complete-job)
  get("/v1/jobs/:jobid/result") {
    ensureAuth { implicit context =>
      params.get("collection").flatMap(ArchCollection.get) match {
        case Some(collection) =>
          val jobId = params("jobid")
          val sample = params.get("sample").contains("true")
          JobManager.getInstanceOrGlobal(
            jobId,
            DerivationJobConf
              .collection(collection, sample = sample, global = false),
            Some(
              DerivationJobConf
                .collection(collection, sample = sample, global = true))) match {
            case Some(instance) =>
              WasapiController.files(
                instance,
                baseDownloadUrl =
                  s"${ArchConf.baseUrl}/files/download/${collection.id}/${instance.job.id}",
                page = params
                  .get("page")
                  .flatMap(p => Try(p.toInt).toOption)
                  .filter(_ >= 1)
                  .getOrElse(1))
            case None =>
              NotFound()
          }
        case None =>
          NotFound("collection parameter missing")
      }
    }
  }
}

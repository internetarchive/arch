package org.archive.webservices.ars

import _root_.io.circe._
import _root_.io.circe.syntax._
import org.apache.http.client.utils.URIBuilder
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.model.{ArchCollection, ArchConf, DerivativeOutput}
import org.archive.webservices.ars.model.api.{WasapiResponse, WasapiResponseFile}
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobInstance, JobManager}
import org.scalatra
import org.scalatra.{ActionResult, NotFound, Ok}

import javax.servlet.http.HttpServletRequest
import scala.collection.immutable.ListMap
import scala.util.Try

object WasapiController {
  val FixedPageSize = 100

  def files(
      instance: DerivationJobInstance,
      downloadUrl: String,
      params: scalatra.Params,
      addSample: Boolean = true)(implicit request: HttpServletRequest): ActionResult = {
    val page = params
      .get("page")
      .flatMap(p => Try(p.toInt).toOption)
      .filter(_ >= 1)
      .getOrElse(1)
    var files = Seq.empty[DerivativeOutput]
    val count = instance.outFiles
      .grouped(WasapiController.FixedPageSize)
      .zipWithIndex
      .map { case (pageItems, idx) =>
        if (idx == page - 1) files = pageItems
        pageItems.toIterator.size
      }
      .sum
    val pages = (count.toDouble / WasapiController.FixedPageSize).ceil.toInt
    var uriBuilder = new URIBuilder(request.getRequestURI)
    if (InputSpec.isCollectionBased(instance.conf.inputSpec)) {
      uriBuilder = uriBuilder.setParameter("collection", instance.conf.inputSpec.collectionId)
    }
    if (addSample && instance.conf.isSample)
      uriBuilder = uriBuilder.setParameter("sample", "true")
    Ok(
      WasapiResponse(
        count = count,
        next = (
          if (page < pages)
            Some(uriBuilder.setParameter("page", (page + 1).toString).build().toString)
          else None),
        previous = (
          if (page > 1)
            Some(uriBuilder.setParameter("page", (page - 1).min(pages).toString).build().toString)
          else None),
        files = files.map(file =>
          WasapiResponseFile(
            filename = file.filename,
            filetype = file.fileType,
            checksums = file.checksums,
            locations = Seq(downloadUrl + "/" + file.filename + (
              if (addSample && instance.conf.isSample) "?sample=true&access=" else "?access="
            ) + file.accessToken),
            size = file.size,
            collection = if (InputSpec.isCollectionBased(instance.conf.inputSpec)) Some(instance.conf.inputSpec.collectionId) else None
          )
        )
      ).toJson,
      Map("Content-Type" -> "application/json"))
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
                ArchConf.baseUrl + "/files/download/" + collection.id + "/" + instance.job.id,
                params)
            case None =>
              NotFound()
          }
        case None =>
          NotFound("collection parameter missing")
      }
    }
  }
}

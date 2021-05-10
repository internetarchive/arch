package org.archive.webservices.ars

import org.scalatra.{NotFound, NotImplemented, Ok}
import _root_.io.circe.syntax._
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArsCloudCollection, ArsCloudConf}
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager, ProcessingState}
import org.archive.webservices.ars.util.CacheUtil

class ApiController extends BaseController {
  get("/runjob/:jobid/:collectionid") {
    val collectionId = params("collectionid")
    ensureLogin(redirect = false, useSession = true, validateCollection = Some(collectionId)) {
      _ =>
        JobManager.get(params("jobid")) match {
          case Some(job) =>
            DerivationJobConf.collection(collectionId, params.get("sample").contains("true")) match {
              case Some(conf) =>
                val instance = job.enqueue(conf).getOrElse(job.history(conf))
                Ok(
                  Map(
                    "state" -> instance.stateStr.asJson,
                    "started" -> (instance.state != ProcessingState.NotStarted).asJson,
                    "finished" -> (instance.state == ProcessingState.Finished).asJson).asJson.spaces4,
                  Map("Content-Type" -> "application/json"))
              case None =>
                NotImplemented()
            }
          case None =>
            NotFound()
        }
    }
  }

  get("/jobstate/:jobid/:collectionid") {
    val collectionId = params("collectionid")
    ensureLogin(redirect = false, useSession = true, validateCollection = Some(collectionId)) {
      _ =>
        val jobId = params("jobid")
        DerivationJobConf
          .collection(collectionId, params.get("sample").contains("true"))
          .flatMap(JobManager.getInstance(jobId, _)) match {
          case Some(instance) =>
            Ok(
              Map(
                "state" -> instance.stateStr.asJson,
                "started" -> (instance.state != ProcessingState.NotStarted).asJson,
                "finished" -> (instance.state == ProcessingState.Finished).asJson).asJson.spaces4,
              Map("Content-Type" -> "application/json"))
          case None =>
            NotFound()
        }
    }
  }

  get("/collection/:collectionid") {
    CacheUtil.cacheRequest(request, enabled = ArsCloudConf.production) {
      val collectionId = params("collectionid")

      ensureLogin(redirect = false, useSession = true) { _ =>
        ArsCloudCollection.get(collectionId).flatMap { collection =>
          DerivationJobConf.collection(collection.id).map { conf =>
            (collection, collection.info, IOHelper.sizeStr(conf.inputPath))
          }
        } match {
          case Some((collection, info, sizeStr)) =>
            Ok(
              {
                Map(
                  "id" -> collection.id.asJson,
                  "name" -> collection.name.asJson,
                  "public" -> collection.public.asJson)
                info.lastJobName.map("lastJobName" -> _.asJson).toMap ++
                  info.lastJobTime
                    .map("lastJobTime" -> _.toString.stripSuffix("Z").replace("T", " ").asJson)
                    .toMap ++
                  Map("size" -> sizeStr.asJson)
              }.asJson.spaces4,
              Map("Content-Type" -> "application/json"))
          case None =>
            NotFound()
        }
      }
    }
  }
}

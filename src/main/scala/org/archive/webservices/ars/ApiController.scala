package org.archive.webservices.ars

import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.processing.{
  DerivationJobConf,
  DerivationJobInstance,
  JobManager,
  ProcessingState
}
import org.archive.webservices.ars.util.FormatUtil
import org.scalatra.{ActionResult, NotFound, Ok}

import scala.collection.immutable.ListMap

class ApiController extends BaseController {
  private def jobStateResponse(instance: DerivationJobInstance): ActionResult = {
    val jsonMap = ListMap(
      "state" -> instance.stateStr.asJson,
      "started" -> (instance.state != ProcessingState.NotStarted).asJson,
      "finished" -> (instance.state == ProcessingState.Finished).asJson,
      "failed" -> (instance.state == ProcessingState.Failed).asJson) ++ {
      val active = instance.active
      Seq("activeStage" -> active.job.name.asJson, "activeState" -> active.stateStr.asJson) ++ {
        active.queue match {
          case Some(queue) =>
            Seq("queue" -> queue.name.asJson, "queuePos" -> active.queueIndex.asJson)
          case None => Seq.empty
        }
      }
    } ++ {
      val info = instance.info
      info.startTime.map(FormatUtil.instantTimeString).map("startTime" -> _.asJson).toSeq ++
        info.finishedTime.map(FormatUtil.instantTimeString).map("finishedTime" -> _.asJson).toSeq
    }
    Ok(jsonMap.asJson.spaces4, Map("Content-Type" -> "application/json"))
  }

  get("/runjob/:jobid/:collectionid") {
    val collectionId = params("collectionid")
    ensureLogin(redirect = false, useSession = true, validateCollection = Some(collectionId)) {
      _ =>
        JobManager.get(params("jobid")) match {
          case Some(job) =>
            DerivationJobConf.collection(collectionId, params.get("sample").contains("true")) match {
              case Some(conf) =>
                jobStateResponse(job.enqueue(conf).getOrElse(job.history(conf)))
              case None =>
                NotFound()
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
            jobStateResponse(instance)
          case None =>
            NotFound()
        }
    }
  }

  get("/collection/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { _ =>
      ArchCollection.get(params("collectionid")) match {
        case Some(collection) =>
          collection.ensureStats()
          val info = collection.info
          Ok(
            {
              ListMap(
                "id" -> collection.id.asJson,
                "name" -> collection.name.asJson,
                "public" -> collection.public.asJson)
              info.lastJobName.map("lastJobName" -> _.asJson).toMap ++
                info.lastJobTime
                  .map("lastJobTime" -> FormatUtil.instantTimeString(_).asJson)
                  .toMap ++
                Map("size" -> FormatUtil.formatBytes(collection.size).asJson)
            }.asJson.spaces4,
            Map("Content-Type" -> "application/json"))
        case None =>
          NotFound()
      }
    }
  }
}

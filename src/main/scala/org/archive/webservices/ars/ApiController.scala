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
      Seq("activeStage" -> active.job.stage.asJson, "activeState" -> active.stateStr.asJson) ++ {
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

  private def runJob(
      collectionId: String,
      jobId: String,
      sample: Boolean,
      rerun: Boolean = false): ActionResult = {
    ensureLogin(redirect = false, useSession = true) { user =>
      {
        for {
          collection <- ArchCollection.get(collectionId)
          job <- JobManager.get(jobId)
          conf <- DerivationJobConf.collection(collectionId, sample)
        } yield {
          if (rerun) job.reset(conf)
          val history = job.history(conf)
          val queued =
            if (history.state == ProcessingState.NotStarted) job.enqueue(conf) else None
          queued match {
            case Some(instance) =>
              instance.user = Some(user)
              instance.collection = Some(collection)
              jobStateResponse(instance)
            case None => jobStateResponse(history)
          }
        }
      }.getOrElse(NotFound())
    }
  }

  get("/runjob/:jobid/:collectionid") {
    runJob(params("collectionid"), params("jobid"), params.get("sample").contains("true"))
  }

  get("/rerunjob/:jobid/:collectionid") {
    runJob(
      params("collectionid"),
      params("jobid"),
      params.get("sample").contains("true"),
      rerun = true)
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
                "public" -> collection.public.asJson) ++ {
                info.lastJobId.map("lastJobId" -> _.asJson).toMap
              } ++ {
                info.lastJobSample.map("lastJobSample" -> _.asJson).toMap
              } ++ {
                info.lastJobName.map("lastJobName" -> _.asJson).toMap
              } ++ {
                info.lastJobTime
                  .map("lastJobTime" -> FormatUtil.instantTimeString(_).asJson)
                  .toMap
              } ++ {
                ListMap(
                  "size" -> FormatUtil.formatBytes(collection.size).asJson,
                  "sortSize" -> collection.size.asJson,
                  "seeds" -> collection.seeds.asJson,
                  "lastCrawlDate" -> collection.lastCrawlDate.asJson)
              }
            }.asJson.spaces4,
            Map("Content-Type" -> "application/json"))
        case None =>
          NotFound()
      }
    }
  }
}

package org.archive.webservices.ars

import _root_.io.circe._
import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.util.FormatUtil
import org.scalatra._

import scala.collection.immutable.ListMap

class ApiController extends BaseController {
  private def jobStateJson(instance: DerivationJobInstance): Json = {
    ListMap(
      "id" -> instance.job.id.asJson,
      "name" -> instance.job.name.asJson,
      "sample" -> instance.conf.sample.asJson,
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
  }.asJson

  private def jobStateResponse(instance: DerivationJobInstance): ActionResult = {
    Ok(jobStateJson(instance).spaces4, Map("Content-Type" -> "application/json"))
  }

  private def runJob(
      job: DerivationJob,
      collection: ArchCollection,
      conf: DerivationJobConf,
      rerun: Boolean)(implicit context: RequestContext): ActionResult = {
    if (rerun) job.reset(conf)
    val history = job.history(conf)
    val queued =
      if (history.state == ProcessingState.NotStarted) job.enqueue(conf, { instance =>
        instance.user = context.loggedInOpt
        instance.collection = Some(collection)
      })
      else None
    queued match {
      case Some(instance) => jobStateResponse(instance)
      case None => jobStateResponse(history)
    }
  }

  private def runJob(
      collectionId: String,
      jobId: String,
      sample: Boolean,
      rerun: Boolean = false,
      params: DerivationJobParameters = DerivationJobParameters.Empty)(
      implicit context: RequestContext): ActionResult = {
    for {
      collection <- ArchCollection.get(collectionId)
      job <- JobManager.get(jobId)
      conf <- DerivationJobConf.collection(collectionId, sample)
    } yield {
      runJob(job, collection, if (params.isEmpty) conf else conf.copy(params = params), rerun)
    }
  }.getOrElse(NotFound())

  get("/runjob/:jobid/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      runJob(
        params("collectionid"),
        params("jobid"),
        sample = params.get("sample").contains("true"),
        rerun = params.get("rerun").contains("true"))
    }
  }

  post("/runjob/:jobid/:collectionid") {
    ensureLogin(redirect = false, useSession = true, userId = params.get("user")) {
      implicit context =>
        DerivationJobParameters.fromJson(request.body) match {
          case Some(p) =>
            val collectionId = params("collectionid")
            val rerun = params.get("rerun").contains("true")
            params("jobid") match {
              case jobId if jobId == UserDefinedQuery.id => {
                for {
                  collection <- ArchCollection.get(collectionId)
                  conf <- DerivationJobConf.userDefinedQuery(collectionId, p)
                } yield {
                  runJob(UserDefinedQuery, collection, conf, rerun = rerun)
                }
              }.getOrElse(NotFound())
              case jobId =>
                val sample = params.get("sample").contains("true")
                runJob(collectionId, jobId, sample = sample, rerun = rerun, params = p)
            }
          case None =>
            BadRequest("Invalid POST body, not a valid JSON job parameters object.")
        }
    }
  }

  get("/rerunjob/:jobid/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      runJob(
        params("collectionid"),
        params("jobid"),
        params.get("sample").contains("true"),
        rerun = true)
    }
  }

  get("/rerun-failed") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      if (context.isAdmin) {
        JobStateManager.rerunFailed()
        Found(Arch.BaseUrl + "/admin/logs/running")
      } else Forbidden()
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

  get("/jobstates/:collectionid") {
    val collectionId = params("collectionid")
    ensureLogin(redirect = false, useSession = true, validateCollection = Some(collectionId)) {
      _ =>
        val active = JobManager.getByCollection(collectionId)
        val instances = if (params.get("all").contains("true")) {
          active ++ DerivationJobConf.collection(collectionId).toSeq.flatMap { conf =>
            val jobs = active.filter(_.conf == conf).map(_.job)
            JobManager.userJobs.filter(!jobs.contains(_)).map(_.history(conf))
          } ++ DerivationJobConf.collection(collectionId, sample = true).toSeq.flatMap { conf =>
            val jobs = active.filter(_.conf == conf).map(_.job)
            JobManager.userJobs.filter(!jobs.contains(_)).map(_.history(conf))
          }
        } else active
        val states = instances.toSeq
          .sortBy(instance => (instance.job.name.toLowerCase, instance.conf.serialize))
          .map(jobStateJson)
        Ok(states.asJson.spaces4, Map("Content-Type" -> "application/json"))
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

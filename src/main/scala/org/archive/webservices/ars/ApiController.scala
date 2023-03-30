package org.archive.webservices.ars

import _root_.io.circe._
import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionInfo}
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery
import org.archive.webservices.ars.util.FormatUtil
import org.archive.webservices.sparkling.io.HdfsIO
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
      conf <- DerivationJobConf.collection(collection, sample)
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
                UserDefinedQuery.validateParams(p).map(e => BadRequest(e)).orElse {
                  for {
                    collection <- ArchCollection.get(collectionId)
                    conf <- DerivationJobConf.userDefinedQuery(collection, p)
                  } yield {
                    runJob(UserDefinedQuery, collection, conf, rerun = rerun)
                  }
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
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      ArchCollection
        .get(params("collectionid"))
        .flatMap { collection =>
          val jobId = params("jobid")
          val sample = params.get("sample").contains("true")
          DerivationJobConf
            .collection(collection, sample = sample)
            .flatMap(
              JobManager.getInstanceOrGlobal(
                jobId,
                _,
                DerivationJobConf
                  .collection(collection, sample = sample, global = true)))
            .map { instance =>
              jobStateResponse(instance)
            }
        }
        .getOrElse(NotFound())
    }
  }

  get("/jobstates/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      ArchCollection
        .get(params("collectionid"))
        .map { collection =>
          val active = JobManager.getByCollection(collection.id)
          val instances = if (params.get("all").contains("true")) {
            active ++ Seq(false, true).flatMap { sample =>
              val conf = DerivationJobConf.collection(collection, sample = sample)
              val jobsIds =
                conf
                  .map(_.outputPath)
                  .toSeq
                  .flatMap(p => HdfsIO.files(p + "/*", recursive = false).map(_.split('/').last))
                  .toSet
              val globalConf =
                DerivationJobConf.collection(collection, sample = sample, global = true)
              val globalJobIds = globalConf
                .map(_.outputPath)
                .toSeq
                .flatMap(p => HdfsIO.files(p + "/*", recursive = false).map(_.split('/').last))
                .toSet -- jobsIds
              conf.toSeq.flatMap { c =>
                val jobs = active.filter(_.conf == c).map(_.job)
                JobManager.userJobs.filter(!jobs.contains(_)).map { job =>
                  if (jobsIds.contains(job.id)) job.history(c)
                  else
                    globalConf
                      .filter(_ => globalJobIds.contains(job.id))
                      .map(job.history)
                      .getOrElse {
                        DerivationJobInstance(job, c)
                      }
                }
              }
            }
          } else active
          val states = instances.toSeq
            .sortBy(instance => (instance.job.name.toLowerCase, instance.conf.serialize))
            .map(jobStateJson)
          Ok(states.asJson.spaces4, Map("Content-Type" -> "application/json"))
        }
        .getOrElse(NotFound())
    }
  }

  get("/collection/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val collectionId = params("collectionid")
      (for {
        collection <- ArchCollection.get(collectionId)
        info <- ArchCollectionInfo.get(collectionId)
      } yield {
        collection.ensureStats()
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
      }).getOrElse(NotFound())
    }
  }
}

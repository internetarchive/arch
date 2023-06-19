package org.archive.webservices.ars
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

import _root_.io.circe._
import _root_.io.circe.parser.parse
import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.{
  ArchCollection, ArchConf, ArchCollectionInfo, ArchJobCategories, PublishedDatasets
}
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.system.{DatasetPublication, UserDefinedQuery}
import org.archive.webservices.ars.util.{DatasetUtil, FormatUtil}
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

  private def conf(
      job: DerivationJob,
      collection: ArchCollection,
      sample: Boolean,
      params: DerivationJobParameters)(
      implicit context: RequestContext): Option[DerivationJobConf] = {
    job match {
      case UserDefinedQuery =>
        DerivationJobConf.userDefinedQuery(collection, params, sample)
      case _ =>
        DerivationJobConf.collection(collection, sample).map { conf =>
          if (params.isEmpty) conf else conf.copy(params = params)
        }
    }
  }

  private def prohibited(jobId: String, params: DerivationJobParameters): Boolean = {
    // Enforce prohibited DatasetPublication datasets.
    jobId == DatasetPublication.id && (
      params.get[String]("dataset")
        .flatMap(JobManager.get)
        .map(PublishedDatasets.ProhibitedJobs.contains(_))
        .getOrElse(false)
    )
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
      conf <- conf(job, collection, sample, params)
      if !prohibited(jobId, params)
    } yield {
      job.validateParams(collection, conf).map(e => BadRequest(e)).getOrElse {
        if (rerun) job.reset(conf)
        val history = job.history(conf)
        val queued =
          if (history.state == ProcessingState.NotStarted || (rerun && history.state == ProcessingState.Failed)) {
            job.enqueue(conf, { instance =>
              instance.user = context.loggedInOpt
              instance.collection = Some(collection)
            })
          } else None
        queued match {
          case Some(instance) => jobStateResponse(instance)
          case None => jobStateResponse(history)
        }
      }
    }
  }.getOrElse(NotFound())

  get("/runjob/:jobid/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      runJob(
        ArchCollection.userCollectionId(params("collectionid"), context.user),
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
            runJob(
              ArchCollection.userCollectionId(params("collectionid"), context.user),
              params("jobid"),
              sample = params.get("sample").contains("true"),
              rerun = params.get("rerun").contains("true"),
              params = p)
          case None =>
            BadRequest("Invalid POST body, not a valid JSON job parameters object.")
        }
    }
  }

  get("/rerunjob/:jobid/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      runJob(
        ArchCollection.userCollectionId(params("collectionid"), context.user),
        params("jobid"),
        params.get("sample").contains("true"),
        rerun = true)
    }
  }

  get("/rerun-failed") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      if (context.isAdmin) {
        JobStateManager.rerunFailed()
        Found(ArchConf.baseUrl + "/admin/logs/running")
      } else Forbidden()
    }
  }

  get("/bypass-spark") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      if (context.isAdmin) {
        Ok(SparkJobManager.bypassJobs())
      } else Forbidden()
    }
  }

  get("/jobstate/:jobid/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      ArchCollection
        .get(ArchCollection.userCollectionId(params("collectionid"), context.user))
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

  get("/available-jobs") {
    ensureLogin(redirect = false, useSession = true) { _ =>
      val categoryJobsMap = JobManager.userJobs.toSeq
        .groupBy(_.category)
        .map {
          case (category, jobs) =>
            category -> jobs.sortBy(_.name.toLowerCase)
        }
      Ok(
        Seq(
          (
            ArchJobCategories.Collection,
            "Discover domain-related patterns and high level information about the documents in a web archive.",
            "/img/collection.png",
            "collection"
          ),
          (
            ArchJobCategories.Network,
            "Explore connections in a web archive visually.",
            "/img/network.png",
            "network"
          ),
          (
            ArchJobCategories.Text,
            "Extract and analyze a web archive as text.",
            "/img/text.png",
            "text"
          ),
          (
            ArchJobCategories.BinaryInformation,
            "Find, describe, and use the files contained within a web archive, based on their format.",
            "/img/file-formats.png",
            "file-formats"
          )
        ).map({
          case (category, categoryDescription, categoryImage, categoryId) => {
            ListMap(
              "categoryName" -> category.name.asJson,
              "categoryDescription" -> categoryDescription.asJson,
              "categoryImage" -> BaseController.staticPath(categoryImage).asJson,
              "categoryId" -> categoryId.asJson,
              "jobs" -> categoryJobsMap.get(category).head.map(job => ListMap(
                "id" -> job.id.asJson,
                "name" -> job.name.asJson,
                "description" -> job.description.asJson,
              )).asJson
            )
          }
        }).asJson.spaces4,
        Map("Content-Type" -> "application/json")
      )
    }
  }

  get("/jobstates/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      ArchCollection
        .get(ArchCollection.userCollectionId(params("collectionid"), context.user))
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

  private def collectionJson(collection: ArchCollection, user: ArchUser): Json = {
    val info = ArchCollectionInfo.get(collection.id)
    ListMap(
      "id" -> collection.userUrlId(user.id).asJson,
      "name" -> collection.name.asJson,
      "public" -> collection.public.asJson,
      "size" -> FormatUtil.formatBytes(collection.size).asJson,
      "sortSize" -> collection.size.asJson,
      "seeds" -> collection.seeds.asJson,
      "lastCrawlDate" -> Option(collection.lastCrawlDate).filter(_.nonEmpty).asJson,
      "lastJobId" -> info.map(_.lastJobId).asJson,
      "lastJobSample" -> info.map(_.lastJobSample).asJson,
      "lastJobName" -> info.map(_.lastJobName).asJson,
      "lastJobTime" -> info.map(_.lastJobTime.map(FormatUtil.instantTimeString)).asJson,
    )
  }.asJson

  get("/collections") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val collections = ArchCollection.userCollections(context.user)
      Ok(
        collections.map(collection => {
          collection.ensureStats()
          collectionJson(collection, context.user)
        }).asJson,
        Map("Content-Type" -> "application/json"))
    }
  }

  get("/datasets") {
    ensureLogin(redirect = false, useSession = true) { context =>
      val collectionIds = multiParams("collectionId").map(ArchCollection.userCollectionId(_, context.user))
      val states = multiParams("state")
      val notStates = multiParams("state!")
      val jobs = multiParams("job")
      val sample = multiParams("sample")
      val user = context.user
      val datasets =
        ArchCollection.userCollections(user)
        // Apply collections filter.
        .filter(col => if (collectionIds.isEmpty) true else collectionIds.contains(col.id))
        // Get (collection, [sample]userConf, [sample]globalConf) tuples
        .flatMap(col =>
          // Apply sample filter.
          (if (sample.isEmpty) Set(true, false) else Seq(sample.contains("true"))).map(sample =>
            (
              col,
              DerivationJobConf.collection(col, sample = sample, global = false).get,
              DerivationJobConf.collection(col, sample = sample, global = true),
            )
          )
        )
        // Get (collection, jobInstance)
        .flatMap{case (col, userConf, globalConf) =>
          JobManager.userJobs
          // Apply jobs filter.
          .filter(job => if (jobs.isEmpty) true else jobs.contains(job.id))
          .map(job => {
            (
              col,
              JobManager.getInstanceOrGlobal(job.id, userConf, globalConf).get
            )
          })
        }
        // Apply states filters.
        .filter(x => if (states.isEmpty) true else states.contains(x._2.stateStr))
        .filter(x => if (notStates.isEmpty) true else !notStates.contains(x._2.stateStr))
        // Sort by state asc, finishedTime desc.
        .sortBy{x => (
          x._2.state,
          - x._2.info.finishedTime.getOrElse(Instant.ofEpochSecond(0)).getEpochSecond
        )}
        .map{
          case (collection, jobInstance) =>
            ListMap(
              "id" -> DatasetUtil.formatId(collection.userUrlId(context.user.id), jobInstance.job).asJson,
              "collectionId" -> collection.userUrlId(user.id).asJson,
              "collectionName" -> collection.name.asJson,
              "isSample" -> (jobInstance.conf.sample != -1).asJson,
              "jobId" -> jobInstance.job.id.asJson,
              "category" -> jobInstance.job.category.name.asJson,
              "name" -> jobInstance.job.name.asJson,
              "sample" -> jobInstance.conf.sample.asJson,
              "state" -> jobInstance.stateStr.asJson,
              "numFiles" -> jobInstance.outFiles.size.asJson,
            ) ++
            jobInstance.info.startTime
              .map(FormatUtil.instantTimeString)
              .map("startTime" -> _.asJson)
              .toSeq ++
            jobInstance.info.finishedTime
              .map(FormatUtil.instantTimeString)
              .map("finishedTime" -> _.asJson)
              .toSeq
        }
      Ok(datasets.asJson, Map("Content-Type" -> "application/json"))
    }
  }

  get("/collection/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collectionid"), context.user)
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

  get("/petabox/:collectionid/metadata/:item") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val item = params("item")
      val collectionId = ArchCollection.userCollectionId(params("collectionid"), context.user)
      val collectionItems = ArchCollection.get(collectionId).toSet.flatMap {
        collection =>
          PublishedDatasets.collectionItems(collection)
      }
      if (collectionItems.contains(item)) {
        PublishedDatasets
          .metadata(item)
          .map { metadata =>
            Ok(
              metadata
                .mapValues { values => values.asJson }
                .asJson
                .spaces4,
              Map("Content-Type" -> "application/json"))
          }
          .getOrElse(NotFound())
      } else NotFound()
    }
  }

  post("/petabox/:collectionid/metadata/:item") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val item = params("item")
      val collectionId = ArchCollection.userCollectionId(params("collectionid"), context.user)
      val collectionItems = ArchCollection.get(collectionId).toSet.flatMap {
        collection =>
          PublishedDatasets.collectionItems(collection)
      }
      if (collectionItems.contains(item)) {
        parse(request.body).toOption
          .map(PublishedDatasets.parseJsonMetadata)
          .map { metadata =>
            PublishedDatasets.validateMetadata(metadata) match {
              case Some(error) => BadRequest(error)
              case None =>
                if (PublishedDatasets.updateItem(item, metadata)) {
                  Ok()
                } else InternalServerError("Updating metadata failed.")
            }
          }
          .getOrElse(BadRequest("Invalid metadata JSON."))
      } else NotFound()
    }
  }

  get("/petabox/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      ArchCollection
        .get(ArchCollection.userCollectionId(params("collectionid"), context.user))
        .map { collection =>
          Ok(
            PublishedDatasets.collectionInfoJson(collection),
            Map("Content-Type" -> "application/json"))
        }
        .getOrElse(Forbidden())
    }
  }

  get("/petabox/:collectionid/:jobid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val jobId = params("jobid")
      val sample = params.get("sample").contains("true")
      ArchCollection
        .get(ArchCollection.userCollectionId(params("collectionid"), context.user))
        .flatMap { collection =>
          val dataset = PublishedDatasets.dataset(jobId, collection, sample)
          dataset.flatMap(PublishedDatasets.jobItem)
        }
        .map { info =>
          Ok(info.toJson(includeItem = true).spaces4, Map("Content-Type" -> "application/json"))
        }
        .getOrElse(NotFound())
    }
  }
}

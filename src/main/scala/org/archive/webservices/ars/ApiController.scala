package org.archive.webservices.ars

import _root_.io.circe._
import _root_.io.circe.parser.parse
import _root_.io.circe.syntax._
import org.archive.webservices.ars.model._
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.api.{Collection, Dataset}
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.system.{DatasetPublication, UserDefinedQuery}
import org.archive.webservices.ars.util.FormatUtil
import org.archive.webservices.sparkling.io.HdfsIO
import org.scalatra._

import java.time.Instant
import java.lang.reflect.Field
import scala.collection.immutable.ListMap
import scala.reflect.{ClassTag, classTag}
import scala.util.{Try, Success}

class ApiController extends BaseController {
  private def filterAndSerialize[T: ClassTag: Encoder](objs: Seq[T]): Json = {
    // Create an object class { fieldName -> Field } map.
    val fieldMap = classTag[T].runtimeClass.getDeclaredFields
      .map(field => {
        field.setAccessible(true)
        (field.getName, field)
      })
      .toMap
    // Attempt to parse string-type param values per the corresponding
    // object field type.
    val parsedMultiParams =
      (multiParams.keys.toSet
        -- Set("distinct", "limit", "offset", "search", "sort")).flatMap(paramKey =>
        (paramKey.charAt(paramKey.size - 1) match {
          case '!' => (paramKey.slice(0, paramKey.size - 1), Some('!'))
          case _ => (paramKey, None)
        }) match {
          case (fieldName, opChar) =>
            for {
              field <- fieldMap.get(fieldName)
              paramValues <- multiParams.get(paramKey)
            } yield (
              fieldName,
              opChar,
              field.getType match {
                // Boolean
                case t if t == classOf[Boolean] => paramValues.map { _ == "true" }
                // Int
                case t if t == classOf[Int] =>
                  paramValues.map(s => Try(s.toInt)).collect { case Success(x) => x }
                // Long
                case t if t == classOf[Long] =>
                  paramValues.map(s => Try(s.toLong)).collect { case Success(x) => x }
                // Option[String]
                case t if t == classOf[Option[String]] =>
                  paramValues.map { paramValue =>
                    if (paramValue == "null") None else Some(paramValue)
                  }
                // String / Default
                case _ => paramValues
              })
        })
    // Apply field filters.
    var filtered = objs.filter(obj => {
      parsedMultiParams.forall { case (paramKey, opChar, paramValues) =>
        fieldMap
          .get(paramKey)
          .map(_.get(obj))
          .map(objValue =>
            opChar match {
              case Some('!') => !paramValues.contains(objValue)
              case _ => paramValues.contains(objValue)
            })
          .getOrElse(true)
      } && params
        .get("search")
        .map(searchTerm => {
          // Filter by any String-type field that contains the search term.
          val lowerSearchTerm = searchTerm.toLowerCase
          fieldMap.values.exists(field =>
            field.getType match {
              case t if t == classOf[String] =>
                field.get(obj).asInstanceOf[String].toLowerCase.contains(lowerSearchTerm)
              case _ => false
            })
        })
        .getOrElse(true)
    })

    // Helper to get the ordering for the specified field.
    def getOrdering(field: Field, reverse: Boolean) = {
      val ordering = field.getType match {
        case t if t == classOf[Boolean] => Ordering.by[Object, Boolean](_.asInstanceOf[Boolean])
        case t if t == classOf[Int] => Ordering.by[Object, Int](_.asInstanceOf[Int])
        case t if t == classOf[Long] => Ordering.by[Object, Long](_.asInstanceOf[Long])
        case t if t == classOf[Option[String]] =>
          Ordering.by[Object, Option[String]](_.asInstanceOf[Option[String]])
        case _ => Ordering.by[Object, String](_.asInstanceOf[String])
      }
      if (reverse) ordering.reverse else ordering
    }

    if (params.get("distinct").isEmpty) {
      // Return normal, paginated, response.
      val (sortField, reverseSort) = Option(params.get("sort").getOrElse("id"))
        .map(s => if (s.startsWith("-")) (s.slice(1, s.size), true) else (s, false))
        .map { case (fieldName, reverse) => (fieldMap.get(fieldName).get, reverse) }
        .get
      Map(
        "count" -> filtered.size.asJson,
        "results" ->
          filtered
            .sortBy(sortField.get)(getOrdering(sortField, reverseSort))
            .drop(params.getAsOrElse[Int]("offset", 0))
            .take(params.getAsOrElse[Int]("limit", Int.MaxValue))
            .map(_.asJson)
            .asJson).asJson
    } else {
      // Return distinct response with results comprising a flat list of
      // sorted, distinct values.
      val results = params
        .get("distinct")
        .map(k => {
          val sortField = fieldMap.get(k).get
          filtered
            .sortBy(sortField.get)(getOrdering(sortField, false))
            .flatMap(_.asJson.findAllByKey(k))
            .distinct
        })
      Map("count" -> results.size.asJson, "results" -> results.asJson).asJson
    }
  }

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
      params: DerivationJobParameters)(implicit
      context: RequestContext): Option[DerivationJobConf] = {
    job match {
      case UserDefinedQuery =>
        DerivationJobConf.userDefinedQuery(collection, params, sample)
      case _ =>
        val conf = DerivationJobConf.collection(collection, sample)
        Some(if (params.isEmpty) conf else conf.copy(params = params))
    }
  }

  private def runJob(
      collectionId: String,
      jobId: String,
      sample: Boolean,
      rerun: Boolean = false,
      params: DerivationJobParameters = DerivationJobParameters.Empty)(implicit
      context: RequestContext): ActionResult = {
    for {
      collection <- ArchCollection.get(collectionId)
      job <- JobManager.get(jobId)
      conf <- conf(job, collection, sample, params)
    } yield {
      job.validateParams(collection, conf).map(e => BadRequest(e)).getOrElse {
        if (rerun) job.reset(conf)
        val history = job.history(conf)
        val queued =
          if (history.state == ProcessingState.NotStarted || (rerun && history.state == ProcessingState.Failed)) {
            job.enqueue(
              conf,
              { instance =>
                instance.user = context.loggedInOpt
                instance.collection = collection
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
          DerivationJobConf.collectionInstance(jobId, collection, sample).map(jobStateResponse)
        }
        .getOrElse(NotFound())
    }
  }

  get("/available-jobs") {
    ensureLogin(redirect = false, useSession = true) { _ =>
      val categoryJobsMap = JobManager.userJobs.toSeq
        .groupBy(_.category)
        .map { case (category, jobs) =>
          category -> jobs.sortBy(_.name.toLowerCase)
        }
      Ok(
        Seq(
          (
            ArchJobCategories.Collection,
            "Discover domain-related patterns and high level information about the documents in a web archive.",
            "/img/collection.png",
            "collection"),
          (
            ArchJobCategories.Network,
            "Explore connections in a web archive visually.",
            "/img/network.png",
            "network"),
          (
            ArchJobCategories.Text,
            "Extract and analyze a web archive as text.",
            "/img/text.png",
            "text"),
          (
            ArchJobCategories.BinaryInformation,
            "Find, describe, and use the files contained within a web archive, based on their format.",
            "/img/file-formats.png",
            "file-formats"))
          .map({
            case (category, categoryDescription, categoryImage, categoryId) => {
              ListMap(
                "categoryName" -> category.name.asJson,
                "categoryDescription" -> categoryDescription.asJson,
                "categoryImage" -> BaseController.staticPath(categoryImage).asJson,
                "categoryId" -> categoryId.asJson,
                "jobs" -> categoryJobsMap
                  .get(category)
                  .head
                  .map(job =>
                    ListMap(
                      "id" -> job.id.asJson,
                      "name" -> job.name.asJson,
                      "description" -> job.description.asJson))
                  .asJson)
            }
          })
          .asJson
          .spaces4,
        Map("Content-Type" -> "application/json"))
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
              val jobsIds = Seq(conf.outputPath)
                .flatMap(p => HdfsIO.files(p + "/*", recursive = false).map(_.split('/').last))
                .toSet
              val globalConf =
                DerivationJobConf.collection(collection, sample = sample, global = true)
              val globalJobIds = Seq(globalConf.outputPath)
                .flatMap(p => HdfsIO.files(p + "/*", recursive = false).map(_.split('/').last))
                .toSet -- jobsIds
              Seq(conf).flatMap { c =>
                val jobs = active.filter(_.conf == c).map(_.job)
                JobManager.userJobs.filter(!jobs.contains(_)).map { job =>
                  if (jobsIds.contains(job.id)) job.history(c)
                  else if (globalJobIds.contains(job.id)) job.history(globalConf)
                  else DerivationJobInstance(job, c)
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

  get("/jobstates") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      if (context.isAdmin) {
        val states = JobManager.registered.toSeq
          .sortBy(instance => (instance.job.name.toLowerCase, instance.conf.serialize))
          .map(jobStateJson)
        Ok(states.asJson.spaces4, Map("Content-Type" -> "application/json"))
      } else Forbidden()
    }
  }

  get("/collections") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val collections = ArchCollection.userCollections(context.user)
      Ok(
        filterAndSerialize[Collection](
          collections.map(collection => new Collection(collection, context.user))),
        Map("Content-Type" -> "application/json"))
    }
  }

  get("/datasets") {
    ensureLogin(redirect = false, useSession = true) { context =>
      val user = context.user
      val datasets =
        ArchCollection
          .userCollections(user)
          // Get (collection, [sample]userConf, [sample]globalConf) tuples
          .flatMap(col =>
            Set(true, false).map(sample =>
              (
                col,
                DerivationJobConf.collection(col, sample = sample, global = false),
                DerivationJobConf.collection(col, sample = sample, global = true))))
          // Get (collection, jobInstance)
          .flatMap { case (col, userConf, globalConf) =>
            JobManager.userJobs
              .map(job => {
                (col, JobManager.getInstanceOrGlobal(job.id, userConf, globalConf).get)
              })
          }
          .map { case (collection, jobInstance) =>
            new Dataset(collection, jobInstance, context.user)
          }
      Ok(filterAndSerialize[Dataset](datasets), Map("Content-Type" -> "application/json"))
    }
  }

  get("/collection/:collectionid") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collectionid"), context.user)
      (for {
        collection <- ArchCollection.get(collectionId)
        info <- ArchCollectionInfo.get(collectionId)
      } yield {
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
            } ++ Seq(
              "size" -> FormatUtil.formatBytes(collection.stats.size).asJson,
              "sortSize" -> collection.stats.size.asJson,
              "seeds" -> collection.stats.seeds.asJson,
              "lastCrawlDate" -> collection.stats.lastCrawlDate.asJson)
          }.asJson.spaces4,
          Map("Content-Type" -> "application/json"))
      }).getOrElse(NotFound())
    }
  }

  get("/petabox/:collectionid/metadata/:item") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val item = params("item")
      val collectionId = ArchCollection.userCollectionId(params("collectionid"), context.user)
      val collectionItems = ArchCollection.get(collectionId).toSet.flatMap { collection =>
        PublishedDatasets.collectionItems(collection)
      }
      if (collectionItems.contains(item)) {
        PublishedDatasets
          .metadata(item)
          .map { metadata =>
            Ok(
              metadata
                .mapValues { values =>
                  values.asJson
                }
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
      val collectionItems = ArchCollection.get(collectionId).toSet.flatMap { collection =>
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

  post("/petabox/:collectionid/delete/:item") {
    ensureLogin(redirect = false, useSession = true) { implicit context =>
      val item = params("item")
      ArchCollection
        .get(params("collectionid"))
        .filter { collection =>
          val collectionItems = PublishedDatasets.collectionItems(collection)
          collectionItems.contains(item)
        }
        .map { collection =>
          val doDelete = parse(request.body).toOption
            .flatMap(_.hcursor.get[Boolean]("delete").toOption)
            .getOrElse(false)
          if (doDelete) {
            if (PublishedDatasets.deletePublished(collection, item)) Ok()
            else InternalServerError("Deleting item failed.")
          } else
            BadRequest(
              "In order to confirm the deletion, please send a JSON with boolean key 'delete' set to true.")
        }
        .getOrElse(NotFound())
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

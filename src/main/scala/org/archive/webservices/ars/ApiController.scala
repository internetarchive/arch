package org.archive.webservices.ars

import _root_.io.circe._
import _root_.io.circe.parser.parse
import _root_.io.circe.syntax._
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model._
import org.archive.webservices.ars.model.api.{
  ApiFieldType,
  ApiResponseObject,
  ApiResponseType,
  Collection,
  Dataset,
  DatasetFile,
}
import org.archive.webservices.ars.model.app.RequestContext
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery
import org.archive.webservices.ars.util.{DatasetUtil,FormatUtil}
import org.archive.webservices.sparkling.io.HdfsIO
import org.scalatra._

import scala.collection.immutable.{ListMap, Set}
import scala.util.Try
import scala.util.matching.Regex

object ApiController {
  def jobStateJson(instance: DerivationJobInstance): Json = {
    ListMap(
      "id" -> instance.job.id.asJson,
      "uuid" -> instance.uuid.asJson,
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
      info.started.map(FormatUtil.instantTimeString).map("startTime" -> _.asJson).toSeq ++ {
        info.finished.map(FormatUtil.instantTimeString).map("finishedTime" -> _.asJson).toSeq
      }
    }
  }.asJson

  def jobStateResponse(instance: DerivationJobInstance): ActionResult = {
    Ok(jobStateJson(instance).spaces4, Map("Content-Type" -> "application/json"))
  }
}

class ApiController extends BaseController {
  private val ReservedParams = Set("distinct", "limit", "offset", "search", "sort")

  private def filterAndSerialize[T <: ApiResponseObject[T]](objs: Seq[T])(implicit
      responseType: ApiResponseType[T]): Json = {
    val fields = responseType.fields

    val predicates = (multiParams -- ReservedParams).flatMap { case (paramKey, paramValues) =>
      val positive = !paramKey.endsWith("!")
      val fieldName = if (!positive) paramKey.dropRight(1) else paramKey
      def predicate[A](obj: T, expected: Set[A]): Boolean =
        obj.get[A](fieldName).map(expected.contains).forall(_ == positive)
      fields.get(fieldName).map {
        case ApiFieldType.Boolean => predicate[Boolean](_, paramValues.map(_ == "true").toSet)
        case ApiFieldType.Int =>
          predicate[Int](_, paramValues.flatMap(s => Try(s.toInt).toOption).toSet)
        case ApiFieldType.Long =>
          predicate[Long](_, paramValues.flatMap(s => Try(s.toLong).toOption).toSet)
        case ApiFieldType.String =>
          predicate[String](
            _,
            paramValues
              .filter(_ != null)
              .map(_.trim)
              .filter(_.nonEmpty)
              .filter(_ != "null")
              .toSet)
      }
    }

    val filtered = objs.filter { obj =>
      predicates.forall(_(obj)) && params.get("search").map(_.toLowerCase).forall { searchTerm =>
        // Filter by any String-type field that contains the search term.
        val strings = fields
          .filter(_._2 == ApiFieldType.String)
          .keys
          .flatMap(obj.get[String](_))
          .map(_.toLowerCase)
        strings.exists(_.contains(searchTerm))
      }
    }

    params.get("distinct") match {
      case Some(distinct) =>
        // Return distinct response with results comprising a flat list of sorted, distinct values.
        val results = filtered
          .sorted(responseType.ordering(distinct))
          .flatMap(_.toJson.findAllByKey(distinct))
          .distinct
        Map("count" -> results.size.asJson, "results" -> results.asJson).asJson
      case None =>
        // Return normal, paginated, response.
        val sortQuery = params.get("sort").getOrElse("id")
        val reverseSort = sortQuery.startsWith("-")
        val sortField = if (reverseSort) sortQuery.drop(1) else sortQuery
        val offset = params.getAsOrElse[Int]("offset", 0)
        val limit = params.getAsOrElse[Int]("limit", Int.MaxValue)
        val results = filtered.sorted(responseType.ordering(sortField, reverseSort))
        Map(
          "count" -> results.size.asJson,
          "results" -> results.toIterator
            .drop(offset)
            .map(_.toJson)
            .take(limit)
            .toArray
            .asJson).asJson
    }
  }

  private def conf(
      job: DerivationJob,
      collection: ArchCollection,
      sample: Boolean,
      params: DerivationJobParameters)(implicit
      context: RequestContext): Option[DerivationJobConf] = {
    job match {
      case UserDefinedQuery =>
        DerivationJobConf.userDefinedQuery(
          collection,
          // If input param is specified, insert the requesting user ID into the
          // collection names as necessary.
          params
            .get[Seq[String]]("input")
            .map(collectionNames =>
              params.set("input", collectionNames.map(ArchCollection.userCollectionId)))
            .getOrElse(params),
          sample)
      case _ =>
        Some(DerivationJobConf.collection(collection, params, sample))
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
      job.validateParams(conf).map(e => BadRequest(e)).getOrElse {
        if (rerun) job.reset(conf)
        val history = job.history(conf)
        val queued =
          if (history.state == ProcessingState.NotStarted || (rerun && history.state == ProcessingState.Failed)) {
            job.enqueue(
              conf,
              { instance =>
                instance.user = context.userOpt
              })
          } else None
        queued match {
          case Some(instance) => ApiController.jobStateResponse(instance)
          case None => ApiController.jobStateResponse(history)
        }
      }
    }
  }.getOrElse(NotFound())

  get("/runjob/:jobid/:collectionid") {
    ensureAuth { implicit context =>
      runJob(
        ArchCollection.userCollectionId(params("collectionid"), context.user),
        params("jobid"),
        sample = params.get("sample").contains("true"),
        rerun = params.get("rerun").contains("true"))
    }
  }

  post("/runjob/:jobid/:collectionid") {
    ensureAuth { implicit context =>
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

  post("/runjob/:jobid") {
    ensureAuth { implicit context =>
      parse(request.body).right.toOption.map(_.hcursor) match {
        case Some(cursor) =>
          JobManager
            .get(params("jobid"))
            .flatMap { job =>
              val rerun = params.get("rerun").contains("true")
              val sample = params.get("sample").contains("true")
              val user =
                cursor.get[String]("user").toOption.flatMap(ArchUser.get).orElse(context.userOpt)
              val customOutPath =
                cursor.keys.toSet.flatten.contains(DerivationJobConf.OutputPathConfKey)
              var reservedOutPath = false
              lazy val uuid = cursor.get[String]("uuid").toOption.getOrElse {
                reservedOutPath = !customOutPath && job.generatesOuputput
                DerivationJobInstance.uuid(reserve = reservedOutPath)
              }
              for (conf <- DerivationJobConf.fromJson(
                  cursor,
                  sample,
                  ArchConf.uuidJobOutPath.map(_ + "/" + uuid))) yield {
                job
                  .validateParams(conf)
                  .map { e =>
                    if (reservedOutPath) HdfsIO.delete(conf.outputPath)
                    BadRequest(e)
                  }
                  .getOrElse {
                    if (rerun) job.reset(conf)
                    val history = job.history(uuid, conf)
                    val queued =
                      if (history.state == ProcessingState.NotStarted || (rerun && history.state == ProcessingState.Failed)) {
                        job.enqueue(
                          conf,
                          { instance =>
                            instance.predefUuid = Some(uuid)
                            instance.user = user
                          })
                      } else None
                    queued match {
                      case Some(instance) => ApiController.jobStateResponse(instance)
                      case None => {
                        if (reservedOutPath && history.conf.outputPath != conf.outputPath) {
                          HdfsIO.delete(conf.outputPath)
                        }
                        ApiController.jobStateResponse(history)
                      }
                    }
                  }
              }
            }
            .getOrElse(NotFound())
        case None =>
          BadRequest("Invalid POST body, no valid JSON object.")
      }
    }
  }

  get("/rerunjob/:jobid/:collectionid") {
    ensureAuth { implicit context =>
      runJob(
        ArchCollection.userCollectionId(params("collectionid"), context.user),
        params("jobid"),
        params.get("sample").contains("true"),
        rerun = true)
    }
  }

  get("/rerun-failed") {
    ensureAuth { implicit context =>
      if (context.isAdmin) {
        JobStateManager.rerunFailed()
        Found(ArchConf.baseUrl + "/admin/logs/running")
      } else Forbidden()
    }
  }

  get("/bypass-spark") {
    ensureAuth { implicit context =>
      if (context.isAdmin) {
        Ok(SparkJobManager.bypassJobs())
      } else Forbidden()
    }
  }

  get("/jobstate/:jobid/:collectionid") {
    ensureAuth { implicit context =>
      ArchCollection
        .get(ArchCollection.userCollectionId(params("collectionid"), context.user))
        .flatMap { collection =>
          val jobId = params("jobid")
          val sample = params.get("sample").contains("true")
          DerivationJobConf
            .collectionInstance(jobId, collection, sample)
            .map(ApiController.jobStateResponse)
        }
        .getOrElse(NotFound())
    }
  }

  get("/available-jobs") {
    ensureAuth { _ =>
      Ok(
        JobManager.jobs.values.toSeq
          .groupBy(_.category)
          .map { case (category, jobs) =>
              ListMap(
                "categoryName" -> category.name.asJson,
                "categoryDescription" -> category.description.asJson,
                "jobs" -> jobs.sortBy(_.name.toLowerCase).map { job =>
                  ListMap(
                    "uuid" -> job.uuid.asJson,
                    "name" -> job.name.asJson,
                    "description" -> job.description.asJson,
                    "publishable" -> (!PublishedDatasets.ProhibitedJobs.contains(job)).asJson,
                    "internal" -> (category == ArchJobCategories.System).asJson)
                  .asJson
                }.asJson
              ).asJson
          }
          .asJson
          .spaces4,
        Map("Content-Type" -> "application/json"))
    }
  }

  get("/jobstates/:collectionid") {
    ensureAuth { implicit context =>
      ArchCollection
        .get(ArchCollection.userCollectionId(params("collectionid"), context.user))
        .map { collection =>
          val active = JobManager.getCollectionInstances(collection.id)
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
            .map(ApiController.jobStateJson)
          Ok(states.asJson.spaces4, Map("Content-Type" -> "application/json"))
        }
        .getOrElse(NotFound())
    }
  }

  get("/jobstates") {
    ensureAuth { implicit context =>
      if (context.isAdmin) {
        val states = JobManager.registered.toSeq
          .sortBy(instance => (instance.job.name.toLowerCase, instance.conf.serialize))
          .map(ApiController.jobStateJson)
        Ok(states.asJson.spaces4, Map("Content-Type" -> "application/json"))
      } else Forbidden()
    }
  }

  get("/collections") {
    ensureAuth { implicit context =>
      Ok(
        filterAndSerialize(ArchCollection.userCollections(context.user).map(Collection(_))),
        Map("Content-Type" -> "application/json"))
    }
  }

  get("/datasets") {
    ensureAuth { implicit context =>
      val user = context.user
      val pathUserId = IOHelper.escapePath(user.id)
      val userFiles = HdfsIO.files(
        s"${ArchConf.jobOutPath}/$pathUserId/*/{out,samples}/*/${ArchJobInstanceInfo.InfoFile}",
        recursive = false)
      val globalFiles = HdfsIO.files(
        s"${ArchConf.globalJobOutPath}/*/{out,samples}/*/${ArchJobInstanceInfo.InfoFile}",
        recursive = false)
      val jobPathRegex = new Regex(
        s"^.+/[^/]*/([^/].*)/(out|samples)/([^/]*)/${ArchJobInstanceInfo.InfoFile}$$",
        "collectionId",
        "outOrSamples",
        "jobId")
      val userIdCollectionMap = ArchCollection.userCollections(user).map(c => (c.id, c)).toMap
      val datasets = (userFiles ++ globalFiles)
        .flatMap(jobPathRegex.findFirstMatchIn)
        .map(m => (m.group("collectionId"), m.group("outOrSamples"), m.group("jobId")))
        .toSeq
        .distinct
        .flatMap { case (collectionId, outOrSamples, jobId) =>
          val sample = outOrSamples == "samples"
          for {
            collection <- userIdCollectionMap.get(
              ArchCollection.userCollectionId(collectionId, user))
            instance <- JobManager
              .getInstanceOrGlobal(
                jobId,
                DerivationJobConf.collection(collection, sample = sample, global = false),
                Some(DerivationJobConf.collection(collection, sample = sample, global = true)))
            if instance.job.category != ArchJobCategories.System
          } yield Dataset(collection, instance)
        }
      Ok(filterAndSerialize(datasets), Map("Content-Type" -> "application/json"))
    }
  }

  get("/datasets/:datasetid/files") {
    ensureAuth { implicit context =>
      val user = context.user
      (for {
        (_, job) <- DatasetUtil.parseId(params("datasetid"), user)
      } yield {
        Ok(
          filterAndSerialize(job.outFiles.map(DatasetFile.apply).toSeq),
          Map("Content-Type" -> "application/json"))
      }).getOrElse(NotFound())
    }
  }

  get("/datasets/:datasetid/sample_viz_data") {
    ensureAuth { implicit context =>
      val user = context.user
      (for {
        (_, job) <- DatasetUtil.parseId(params("datasetid"), user)
      } yield {
        job.sampleVizData match {
          case Some(data: SampleVizData) =>
            Ok(
              data.asJson,
              Map("Content-Type" -> "application/json"))
          case _ => NotFound()
        }
      }).getOrElse(NotFound())
    }
  }

  get("/collection/:collectionid") {
    ensureAuth { implicit context =>
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
    ensureAuth { implicit context =>
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
    ensureAuth { implicit context =>
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
                  Ok("Success.")
                } else InternalServerError("Updating metadata failed.")
            }
          }
          .getOrElse(BadRequest("Invalid metadata JSON."))
      } else NotFound()
    }
  }

  post("/petabox/:collectionid/delete/:item") {
    ensureAuth { implicit context =>
      val item = params("item")
      ArchCollection
        .get(ArchCollection.userCollectionId(params("collectionid"), context.user))
        .filter { collection =>
          val collectionItems = PublishedDatasets.collectionItems(collection)
          collectionItems.contains(item)
        }
        .map { collection =>
          val doDelete = parse(request.body).toOption
            .flatMap(_.hcursor.get[Boolean]("delete").toOption)
            .getOrElse(false)
          if (doDelete) {
            if (PublishedDatasets.deletePublished(collection, item)) Ok("Success.")
            else InternalServerError("Deleting item failed.")
          } else {
            BadRequest(
              "In order to confirm the deletion, please send a JSON with boolean key 'delete' set to true.")
          }
        }
        .getOrElse(NotFound())
    }
  }

  get("/petabox/:collectionid") {
    ensureAuth { implicit context =>
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
    ensureAuth { implicit context =>
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

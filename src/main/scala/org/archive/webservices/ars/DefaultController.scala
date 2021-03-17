package org.archive.webservices.ars

import org.archive.helge.sparkling.io.HdfsIO
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArsCloudCollection, ArsCloudConf, ArsCloudJobCategories}
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.archive.webservices.ars.util.CacheUtil
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

import scala.util.Try

class DefaultController extends BaseController with ScalateSupport {
  get("/?") {
    ensureLogin { implicit user =>
      if (user.id == 0) chooseAccount()
      else TemporaryRedirect(relativePath(""))
    }
  }

  get("/:userid/?*") {
    ensureUserBasePath("userid") { _ =>
      TemporaryRedirect("https://partner.archive-it.org" + requestPath)
    }
  }

  get("/:userid/research_services/?*") {
    ensureUserBasePath("userid") { user =>
      val aitCollections = ArsCloudCollection.userCollections(user)
      val collections = aitCollections.flatMap { collection =>
        DerivationJobConf.collection(collection.id).map { conf =>
          (collection, collection.info, IOHelper.sizeStr(conf.inputPath))
        }
      }
      Ok(
        ssp("index", "collections" -> collections, "user" -> user),
        Map("Content-Type" -> "text/html"))
    }
  }

  get("/:userid/research_services/download") {
    ensureUserBasePath("userid") { user =>
      Ok(ssp("download", "user" -> user), Map("Content-Type" -> "text/html"))
    }
  }

  get("/:userid/research_services/learning_resources") {
    ensureUserBasePath("userid") { user =>
      Ok(ssp("learning", "user" -> user), Map("Content-Type" -> "text/html"))
    }
  }

  get("/:userid/research_services/analysis/:collection_id") {
    ensureUserBasePath("userid") { implicit user =>
      val collectionId = params("collection_id")
      (for {
        collection <- ArsCloudCollection.get(collectionId)
        conf <- DerivationJobConf.collection(collectionId)
        sampleConf <- DerivationJobConf.collection(collectionId, sample = true)
      } yield {
        val jobs =
          JobManager.jobs.values.toSeq
            .filter(_.category != ArsCloudJobCategories.None)
            .groupBy(_.category)
            .map {
              case (category, jobs) =>
                category -> jobs.sortBy(_.name).flatMap { job =>
                  for {
                    instance <- JobManager.getInstance(job.id, conf)
                    sampleInstance <- JobManager.getInstance(job.id, sampleConf)
                  } yield (instance, sampleInstance)
                }
            }
        Ok(
          ssp(
            "analysis",
            "breadcrumbs" -> Seq((relativePath("/analysis/" + collectionId), collectionId)),
            "jobs" -> jobs,
            "user" -> user,
            "collection" -> collection),
          Map("Content-Type" -> "text/html"))
      }).getOrElse(NotFound())
    }
  }

  get("/:userid/research_services/analysis/:collection_id/:job_id") {
    CacheUtil.cacheRequest(request, enabled = ArsCloudConf.production) {
      ensureUserBasePath("userid") { implicit user =>
        val collectionId = params("collection_id")
        val jobId = params("job_id")
        (for {
          collection <- ArsCloudCollection.get(collectionId)
          conf <- DerivationJobConf.collection(
            collectionId,
            sample = params.get("sample").contains("true"))
          instance <- JobManager.getInstance(jobId, conf)
        } yield {
          instance.job.templateName match {
            case Some(templateName) =>
              val attributes = Seq(
                "breadcrumbs" -> Seq(
                  (relativePath("/analysis/" + collectionId), collectionId),
                  (relativePath("/analysis/" + collectionId + "/" + jobId), instance.job.name)),
                "user" -> user,
                "collection" -> collection,
                "job" -> instance,
                "files" -> instance.outFiles) ++ instance.templateVariables
              Ok(ssp(templateName, attributes: _*), Map("Content-Type" -> "text/html"))
            case None =>
              NotImplemented()
          }
        }).getOrElse(NotFound())
      }
    }
  }

  get("/:userid/research_services/download/:collection_id/:job_id/:file_name") {
    ensureUserBasePath("userid") { implicit user =>
      val collectionId = params("collection_id")
      val jobId = params("job_id")
      (for {
        collection <- ArsCloudCollection.get(collectionId)
        conf <- DerivationJobConf.collection(
          collectionId,
          sample = params.get("sample").contains("true"))
        instance <- JobManager.getInstance(jobId, conf)
      } yield {
        instance.outFiles.find(_.filename == params("file_name")) match {
          case Some(file) =>
            Ok(
              HdfsIO.open(file.path, decompress = false),
              Map(
                "Content-Type" -> file.mimeType,
                "Content-Disposition" -> ("attachment; filename=" + file.filename)))
          case None =>
            NotImplemented()
        }
      }).getOrElse(NotFound())
    }
  }

  get("/login") {
    val next = Try(params("next")).toOption.filter(_ != null).getOrElse(ArsCloud.BaseUrl)
    Ok(
      ssp("login", "breadcrumbs" -> Seq((ArsCloud.BasePath + "/login", "Login")), "next" -> next),
      Map("Content-Type" -> "text/html"))
  }

  post("/login") {
    val next = Try(params("next")).toOption.filter(_ != null).getOrElse(ArsCloud.BaseUrl)
    try {
      val username = params("username")
      val password = params("password")
      Ait.login(username, password) match {
        case Some(error) =>
          Ok(
            ssp(
              "login",
              "error" -> Some(error),
              "breadcrumbs" -> Seq((ArsCloud.BasePath + "/login", "Login"))),
            Map("Content-Type" -> "text/html"))
        case None =>
          Found(next)
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        NotFound()
    }
  }

  get("/logout") {
    Ait.logout()
    login(ArsCloud.BaseUrl)
  }
}

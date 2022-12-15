package org.archive.webservices.ars

import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf, ArchJobCategories}
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.archive.webservices.ars.util.CacheUtil
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

import scala.util.Try

class DefaultController extends BaseController with ScalateSupport {
  get("/?") {
    ensureLogin { implicit context =>
      if (context.isAdmin) TemporaryRedirect(Arch.BaseUrl + "/admin")
      else TemporaryRedirect(relativePath(""))
    }
  }

  get("/:userid/?*") {
    ensureUserBasePath("userid") { implicit context =>
      TemporaryRedirect(relativePath("")) //TemporaryRedirect("https://partner.archive-it.org" + requestPath)
    }
  }

  get("/:userid/research_services/?*") {
    ensureUserBasePath("userid") { context =>
      val collections = ArchCollection.userCollections(context.viewUser)
      Ok(
        ssp("index", "collections" -> collections, "user" -> context.viewUser),
        Map("Content-Type" -> "text/html"))
    }
  }

  get("/:userid/research_services/:collection_id/analysis") {
    ensureUserBasePath("userid") { implicit context =>
      val collectionId = params("collection_id")
      ArchCollection
        .get(collectionId)
        .map { collection =>
          val jobs =
            JobManager.jobs.values.toSeq
              .filter(_.category != ArchJobCategories.None)
              .groupBy(_.category)
              .map {
                case (category, jobs) =>
                  category -> jobs.sortBy(_.name.toLowerCase)
              }
          Ok(
            ssp(
              "analysis",
              "breadcrumbs" -> Seq(
                (relativePath("/" + collectionId + "/analysis"), collection.name)),
              "jobs" -> jobs,
              "user" -> context.viewUser,
              "collection" -> collection),
            Map("Content-Type" -> "text/html"))
        }
        .getOrElse(NotFound())
    }
  }

  get("/:userid/research_services/:collection_id/jobs") {
    ensureUserBasePath("userid") { implicit context =>
      val collectionId = params("collection_id")
      (for {
        collection <- ArchCollection.get(collectionId)
        conf <- DerivationJobConf.collection(collection.id)
        sampleConf <- DerivationJobConf.collection(collection.id, sample = true)
      } yield {
        val jobs =
          JobManager.jobs.values.toSeq
            .filter(_.category != ArchJobCategories.None)
            .groupBy(_.category)
            .map {
              case (category, jobs) =>
                category -> jobs.sortBy(_.name.toLowerCase).flatMap { job =>
                  for {
                    instance <- JobManager.getInstance(job.id, conf)
                    sampleInstance <- JobManager.getInstance(job.id, sampleConf)
                  } yield (instance, sampleInstance)
                }
            }
        Ok(
          ssp(
            "jobs",
            "breadcrumbs" -> Seq(
              (relativePath("/" + collectionId + "/analysis"), collection.name),
              (relativePath("/" + collectionId + "/jobs"), "Generate Datasets")),
            "jobs" -> jobs,
            "user" -> context.viewUser,
            "collection" -> collection),
          Map("Content-Type" -> "text/html"))
      }).getOrElse(NotFound())
    }
  }

  get("/:userid/research_services/:collection_id/analysis/:job_id") {
    CacheUtil.cacheRequest(request, enabled = ArchConf.production) {
      ensureUserBasePath("userid") { implicit context =>
        val collectionId = params("collection_id")
        val jobId = params("job_id")
        (for {
          collection <- ArchCollection.get(collectionId)
          conf <- DerivationJobConf.collection(
            collectionId,
            sample = params.get("sample").contains("true"))
          instance <- JobManager.getInstance(jobId, conf)
        } yield {
          instance.job.templateName match {
            case Some(templateName) =>
              val attributes = Seq(
                "breadcrumbs" -> Seq(
                  (relativePath("/" + collectionId + "/analysis"), collection.name),
                  (relativePath("/" + collectionId + "/analysis/" + jobId), instance.job.name)),
                "user" -> context.viewUser,
                "collection" -> collection,
                "job" -> instance,
                "files" -> instance.outFiles) ++ instance.templateVariables
              Ok(ssp(templateName, attributes: _*), Map("Content-Type" -> "text/html"))
            case None =>
              NotFound()
          }
        }).getOrElse(NotFound())
      }
    }
  }

  get("/login") {
    val next = Try(params("next")).toOption.filter(_ != null).getOrElse(Arch.BaseUrl)
    Ok(
      ssp("login", "breadcrumbs" -> Seq((Arch.BasePath + "/login", "Login")), "next" -> next),
      Map("Content-Type" -> "text/html"))
  }

  post("/login") {
    val next = Try(params("next")).toOption.filter(_ != null).getOrElse(Arch.BaseUrl)
    try {
      val username = params("username")
      val password = params("password")
      ArchUser.login(username, password) match {
        case Some(error) =>
          Ok(
            ssp(
              "login",
              "error" -> Some(error),
              "next" -> next,
              "breadcrumbs" -> Seq((Arch.BasePath + "/login", "Login"))),
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
    ArchUser.logout()
    login(Arch.BaseUrl)
  }
}

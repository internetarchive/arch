package org.archive.webservices.ars

import org.archive.helge.sparkling.io.HdfsIO
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf, ArchJobCategories}
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.archive.webservices.ars.util.CacheUtil
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

import scala.util.Try

class DefaultController extends BaseController with ScalateSupport {
  get("/?") {
    ensureLogin { implicit user =>
      if (user.isAdmin) TemporaryRedirect(Arch.BaseUrl + "/admin")
      else TemporaryRedirect(relativePath(""))
    }
  }

  get("/:userid/?*") {
    ensureUserBasePath("userid") { implicit user =>
      TemporaryRedirect(relativePath("")) //TemporaryRedirect("https://partner.archive-it.org" + requestPath)
    }
  }

  get("/:userid/research_services/?*") {
    ensureUserBasePath("userid") { user =>
      val collections = ArchCollection.userCollections(user)
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
        collection <- ArchCollection.get(collectionId)
        conf <- DerivationJobConf.collection(collectionId)
        sampleConf <- DerivationJobConf.collection(collectionId, sample = true)
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
    CacheUtil.cacheRequest(request, enabled = ArchConf.production) {
      ensureUserBasePath("userid") { implicit user =>
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
                  (relativePath("/analysis/" + collectionId), collectionId),
                  (relativePath("/analysis/" + collectionId + "/" + jobId), instance.job.name)),
                "user" -> user,
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

  get("/:userid/research_services/download/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val sample = params.get("sample").contains("true")
    val filename = params("file_name")
    params.get("access") match {
      case Some(accessToken) =>
        val jobId = params("job_id")
        (for {
          conf <- DerivationJobConf.collection(collectionId, sample = sample)
          instance <- JobManager.getInstance(jobId, conf)
        } yield {
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) =>
              if (file.accessToken == accessToken) {
                Ok(
                  HdfsIO.open(file.path, decompress = false),
                  Map(
                    "Content-Type" -> file.mimeType,
                    "Content-Disposition" -> ("attachment; filename=" + file.filename)))
              } else Forbidden()
            case None =>
              NotFound()
          }
        }).getOrElse(NotFound())
      case None =>
        ensureUserBasePath(
          "userid",
          redirectOnForbidden = false,
          validateCollection = Some(collectionId)) { implicit user =>
          val jobId = params("job_id")
          (for {
            conf <- DerivationJobConf.collection(collectionId, sample = sample)
            instance <- JobManager.getInstance(jobId, conf)
          } yield {
            instance.outFiles.find(_.filename == filename) match {
              case Some(file) =>
                Ok(
                  HdfsIO.open(file.path, decompress = false),
                  Map(
                    "Content-Type" -> file.mimeType,
                    "Content-Disposition" -> ("attachment; filename=" + file.filename)))
              case None =>
                NotFound()
            }
          }).getOrElse(NotFound())
        }
    }
  }

  get("/:userid/research_services/preview/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val sample = params.get("sample").contains("true")
    val filename = params("file_name")
    ensureLogin(redirect = false, useSession = true, validateCollection = Some(collectionId)) { implicit user =>
      val jobId = params("job_id")
      (for {
        conf <- DerivationJobConf.collection(collectionId, sample = sample)
        instance <- JobManager.getInstance(jobId, conf)
        } yield {
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) =>
              Ok(
                HdfsIO.lines(file.path, n = 51).mkString("\n"),
                Map(
                  "Content-Type" -> file.mimeType))
              case None =>
                NotFound()
          }
        }).getOrElse(NotFound())
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

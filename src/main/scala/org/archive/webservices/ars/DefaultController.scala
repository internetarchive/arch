package org.archive.webservices.ars

import org.archive.helge.sparkling._
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.helge.sparkling.util.StringUtil
import org.archive.webservices.ars.ait.Ait
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
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
      val aitCollections = Ait
        .getJson("/api/collection?limit=100&account=" + user.id) { cursor =>
          cursor.values.map(
            _.map(_.hcursor)
              .flatMap(
                c =>
                  c.get[Int]("id")
                    .right
                    .toOption
                    .map(
                      id =>
                        (
                          "ARCHIVEIT-" + id,
                          c.get[String]("name").right.getOrElse(id.toString),
                          c.get[Boolean]("publicly_visible")
                            .right
                            .getOrElse(id.toString))))
              .toSeq)
        }
        .getOrElse(Seq.empty)
      val collections = aitCollections.flatMap {
        case (id, name, publicly_visible) =>
          DerivationJobConf.collection(id).map { conf =>
            val sizeStr = StringUtil.formatNumber(
              HdfsIO.files(conf.inputPath).map(HdfsIO.length).sum.toDouble / 1.gb,
              2) + " GB"
            (id, name, publicly_visible, sizeStr)
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
      val jobs =
        JobManager.jobs.values.toSeq
          .flatMap { job =>
            JobManager.getInstance(collectionId, job.id)
          }
          .filter(_.job.category != ArsCloudJobCategories.None)
          .groupBy(_.job.category)
          .mapValues(_.sortBy(_.job.name))
      Ok(
        ssp(
          "analysis",
          "breadcrumbs" -> Seq((relativePath("/analysis/" + collectionId), collectionId)),
          "jobs" -> jobs,
          "user" -> user,
          "collectionId" -> collectionId),
        Map("Content-Type" -> "text/html"))
    }
  }

  get("/:userid/research_services/analysis/:collection_id/:job_id") {
    ensureUserBasePath("userid") { implicit user =>
      val collectionId = params("collection_id")
      val jobId = params("job_id")
      JobManager.getInstance(collectionId, jobId) match {
        case Some(instance) =>
          instance.job.templateName match {
            case Some(templateName) =>
              val attributes = Seq(
                "breadcrumbs" -> Seq(
                  (relativePath("/analysis/" + collectionId), collectionId),
                  (relativePath("/analysis/" + collectionId + "/" + jobId), instance.job.name)),
                "user" -> user,
                "collectionId" -> collectionId,
                "job" -> instance.job) ++ instance.templateVariables
              Ok(ssp(templateName, attributes: _*), Map("Content-Type" -> "text/html"))
            case None =>
              NotImplemented()
          }
        case None =>
          NotFound()
      }
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

package org.archive.webservices.ars

import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchJobCategories}
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
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
      val collections = ArchCollection.userCollections(context.user)
      Ok(
        ssp("index", "collections" -> collections, "user" -> context.user),
        Map("Content-Type" -> "text/html"))
    }
  }

  get("/:userid/research_services/:collection_id/?*") {
    Found(requestPath.stripSuffix("/") + "/analysis")
  }

  get("/:userid/research_services/:collection_id/analysis") {
    ensureUserBasePath("userid") { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"))
      ArchCollection
        .get(collectionId)
        .map { collection =>
          Ok(
            ssp(
              "analysis",
              "breadcrumbs" -> Seq(
                (relativePath("/" + collection.userUrlId + "/analysis"), collection.name)),
              "user" -> context.user,
              "collection" -> collection),
            Map("Content-Type" -> "text/html"))
        }
        .getOrElse(NotFound())
    }
  }

  get("/:userid/research_services/:collection_id/subset") {
    ensureUserBasePath("userid") { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"))
      ArchCollection
        .get(collectionId)
        .map { collection =>
          Ok(
            ssp(
              "subset",
              "breadcrumbs" -> Seq(
                (relativePath("/" + collection.userUrlId + "/analysis"), collection.name),
                (relativePath("/" + collection.userUrlId + "/subset"), "Sub-Collection Query")),
              "user" -> context.user,
              "collection" -> collection),
            Map("Content-Type" -> "text/html"))
        }
        .getOrElse(NotFound())
    }
  }

  get("/:userid/research_services/:collection_id/sub-collection-builder") {
    ensureUserBasePath("userid") { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"))
      ArchCollection
        .get(collectionId)
        .map { collection =>
          Ok(
            ssp(
              "sub-collection-builder",
              "breadcrumbs" -> Seq(
                (relativePath("/" + collection.userUrlId + "/analysis"), collection.name),
                (
                  relativePath("/" + collection.userUrlId + "/sub-collection-builder"),
                  "Sub-Collection Builder")),
              "user" -> context.user,
              "collection" -> collection),
            Map("Content-Type" -> "text/html"))
        }
        .getOrElse(NotFound())
    }
  }

  post("/:userid/research_services/:collection_id/sub-collection-builder") {
    ensureUserBasePath("userid") { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"))
      ArchCollection
        .get(collectionId)
        .map { collection =>
          SeeOther(relativePath("/" + collection.userUrlId + "/analysis"))
        }
        .getOrElse(NotFound())
    }
  }

  get("/:userid/research_services/:collection_id/jobs") {
    ensureUserBasePath("userid") { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"))
      (for {
        collection <- ArchCollection.get(collectionId)
      } yield {
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
            "jobs",
            "breadcrumbs" -> Seq(
              (relativePath("/" + collection.userUrlId + "/analysis"), collection.name),
              (relativePath("/" + collection.userUrlId + "/jobs"), "Generate Datasets")),
            "jobs" -> jobs,
            "user" -> context.user,
            "collection" -> collection),
          Map("Content-Type" -> "text/html"))
      }).getOrElse(NotFound())
    }
  }

  get("/:userid/research_services/:collection_id/analysis/:job_id") {
    ensureUserBasePath("userid") { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"))
      val jobId = params("job_id")
      val sample = params.get("sample").contains("true")
      (for {
        collection <- ArchCollection.get(collectionId)
        conf <- DerivationJobConf.collection(collection, sample = sample)
        instance <- JobManager.getInstanceOrGlobal(
          jobId,
          conf,
          DerivationJobConf.collection(collection, sample = sample, global = true))
      } yield {
        instance.job.templateName match {
          case Some(templateName) =>
            val attributes = Seq(
              "breadcrumbs" -> Seq(
                (relativePath("/" + collection.userUrlId + "/analysis"), collection.name),
                (
                  relativePath("/" + collection.userUrlId + "/analysis/" + jobId),
                  instance.job.name)),
              "user" -> context.user,
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

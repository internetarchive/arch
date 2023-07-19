package org.archive.webservices.ars

import org.archive.webservices.ars.BaseController.relativePath
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchConf, PublishedDatasets}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.util.DatasetUtil
import org.scalatra._

import scala.util.Try

private object BreadCrumbs {
  def login: (String, String) = {
    (ViewPathPatterns.reverse(ViewPathPatterns.Login), "Login")
  }

  def collections: (String, String) = {
    (ViewPathPatterns.reverse(ViewPathPatterns.Collections), "Collections")
  }

  def collection(collection: ArchCollection, user: ArchUser): (String, String) = {
    (
      ViewPathPatterns.reverse(
        ViewPathPatterns.Collection,
        Map("collection_id" -> collection.userUrlId(user.id))),
      collection.name)
  }

  def datasets: (String, String) = {
    (ViewPathPatterns.reverse(ViewPathPatterns.Datasets), "Datasets")
  }

  def dataset(datasetId: String, sample: Boolean): (String, String) = {
    (
      ViewPathPatterns.reverse(
        ViewPathPatterns.Dataset,
        Map("dataset_id" -> datasetId, "sample" -> sample.toString)),
      datasetId)
  }
}

class DefaultController extends BaseController {
  get(ViewPathPatterns.Home) {
    ensureLogin { implicit context =>
      Ok(
        ssp(
          "dashboard",
          "breadcrumbs" -> Seq((ViewPathPatterns.reverse(ViewPathPatterns.Home), "Home")),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html"))
    }
  }

  get(ViewPathPatterns.Collections) {
    ensureLogin { implicit context =>
      Ok(
        ssp("collections", "breadcrumbs" -> Seq(BreadCrumbs.collections), "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html"))
    }
  }

  get(ViewPathPatterns.Datasets) {
    ensureLogin { _ =>
      redirect(ViewPathPatterns.reverse(ViewPathPatterns.DatasetExplorer))
    }
  }

  get(ViewPathPatterns.Collection) {
    ensureLogin { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"), context.user)
      ArchCollection
        .get(collectionId)
        .map { collection =>
          Ok(
            ssp(
              "collection-details",
              "breadcrumbs" -> Seq(
                BreadCrumbs.collections,
                BreadCrumbs.collection(collection, context.user),
              ),
              "user" -> context.user,
              "collection" -> collection),
            Map("Content-Type" -> "text/html"))
        }
        .getOrElse(NotFound())
    }
  }

  get("/collections/:collection_id/subset") {
    ensureLogin { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"))
      ArchCollection
        .get(collectionId)
        .map { collection =>
          Ok(
            ssp(
              "subset",
              "breadcrumbs" -> Seq(
                BreadCrumbs.collections,
                BreadCrumbs.collection(collection, context.user),
                (
                  relativePath("/collections/" + collectionId + "/subset"),
                  "Sub-Collection Query")),
              "user" -> context.user,
              "collection" -> collection),
            Map("Content-Type" -> "text/html"))
        }
        .getOrElse(NotFound())
    }
  }

  get(ViewPathPatterns.CustomCollectionBuilder) {
    ensureLogin { implicit context =>
      Ok(
        ssp(
          "sub-collection-builder",
          "breadcrumbs" -> Seq(
            BreadCrumbs.collections,
            (
              ViewPathPatterns.reverse(ViewPathPatterns.CustomCollectionBuilder),
              "Custom Collection Builder"),
          ),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html"))
    }
  }

  post("/collections/sub-collection-builder") {
    ensureLogin { implicit context =>
      val collectionId = ArchCollection.userCollectionId(params("collection_id"))
      ArchCollection
        .get(collectionId)
        .map { collection =>
          SeeOther(relativePath("/" + collection.userUrlId + "/analysis"))
        }
        .getOrElse(NotFound())
    }
  }

  get(ViewPathPatterns.Dataset) {
    ensureLogin { implicit context =>
      val datasetId = params("dataset_id")
      val sample = params.get("sample").contains("true")
      (for {
        (collection, job) <- DatasetUtil.parseId(datasetId, context.user)
        instance <- DerivationJobConf.collectionInstance(job.id, collection, sample)
      } yield {
        instance.job.templateName match {
          case Some(templateName) =>
            val attributes = Seq(
              "breadcrumbs" -> Seq(BreadCrumbs.datasets, BreadCrumbs.dataset(datasetId, sample),
              ),
              "user" -> context.user,
              "collection" -> collection,
              "job" -> instance,
              "files" -> instance.outFiles,
              "publishingEnabled" -> !PublishedDatasets.ProhibitedJobs.contains(instance.job),
            ) ++ instance.templateVariables
            Ok(ssp(templateName, attributes: _*), Map("Content-Type" -> "text/html"))
          case None =>
            NotFound()
        }
      }).getOrElse(NotFound())
    }
  }

  get(ViewPathPatterns.DatasetExplorer) {
    ensureLogin { implicit context =>
      Ok(
        ssp(
          "datasets-explore",
          "breadcrumbs" -> Seq(
            BreadCrumbs.datasets,
            (ViewPathPatterns.reverse(ViewPathPatterns.DatasetExplorer), "Explore"),
          ),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html"))
    }
  }

  get(ViewPathPatterns.GenerateDataset) {
    ensureLogin { implicit context =>
      Ok(
        ssp(
          "datasets-generate",
          "breadcrumbs" -> Seq(
            BreadCrumbs.datasets,
            (ViewPathPatterns.reverse(ViewPathPatterns.GenerateDataset), "Generate"),
          ),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html"))
    }
  }

  get(ViewPathPatterns.Login) {
    val next = Try(params("next")).toOption.filter(_ != null).getOrElse(ArchConf.baseUrl)
    Ok(
      ssp("login", "breadcrumbs" -> Seq(BreadCrumbs.login), "next" -> next),
      Map("Content-Type" -> "text/html"))
  }

  post("/login") {
    val next = Try(params("next")).toOption.filter(_ != null).getOrElse(ArchConf.baseUrl)
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
              "breadcrumbs" -> Seq(BreadCrumbs.login),
            ),
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
    clearMasqueradeUser()
    login(ArchConf.baseUrl)
  }
}

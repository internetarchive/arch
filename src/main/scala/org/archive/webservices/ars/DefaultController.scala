package org.archive.webservices.ars

import org.archive.webservices.ars.BaseController.relativePath
import org.archive.webservices.ars.ViewPathPatterns
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchConf, ArchCollection, ArchJobCategories, PublishedDatasets}
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.archive.webservices.ars.util.DatasetUtil
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

import scala.util.Try

private object BreadCrumbs {
  def Login() = {
    (ViewPathPatterns.reverse(ViewPathPatterns.Login), "Login")
  }

  def Collections() = {
    (ViewPathPatterns.reverse(ViewPathPatterns.Collections), "Collections")
  }

  def Collection(collection: ArchCollection, user: ArchUser) = {
    (
      ViewPathPatterns.reverse(ViewPathPatterns.Collection,
        Map("collection_id" -> collection.userUrlId(user.id))
      ),
      collection.name
    ),
  }

  def Datasets() = {
    (ViewPathPatterns.reverse(ViewPathPatterns.Datasets), "Datasets"),
  }

  def Dataset(datasetId: String, sample: Boolean) = {
    (
      ViewPathPatterns.reverse(
        ViewPathPatterns.Dataset,
        Map("dataset_id" -> datasetId, "sample" -> sample.toString)
      ),
      datasetId
    ),
  }
}

class DefaultController extends BaseController with ScalateSupport {
  get(ViewPathPatterns.Home) {
    ensureLogin { implicit context =>
      Ok(
        ssp(
          "dashboard",
          "breadcrumbs" -> Seq(
            (ViewPathPatterns.reverse(ViewPathPatterns.Home), "Home"),
          ),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html")
      )
    }
  }

  get(ViewPathPatterns.Collections) {
    ensureLogin { implicit context =>
      Ok(
        ssp(
          "collections",
          "breadcrumbs" -> Seq(
            BreadCrumbs.Collections,
          ),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html")
      )
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
          collection.ensureStats()
          Ok(
            ssp(
              "collection-details",
              "breadcrumbs" -> Seq(
                BreadCrumbs.Collections,
                BreadCrumbs.Collection(collection, context.user),
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
                BreadCrumbs.Collections,
                BreadCrumbs.Collection(collection, context.user),
                (relativePath("/collections/" + collectionId + "/subset"), "Sub-Collection Query")
              ),
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
            BreadCrumbs.Collections,
            (ViewPathPatterns.reverse(ViewPathPatterns.CustomCollectionBuilder), "Custom Collection Builder"),
          ),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html")
      )
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

  // TODO - prune this route and associated template
  // get(/:collection_id/jobs) {
  //   ensureLogin { implicit context =>
  //     val collectionId = ArchCollection.userCollectionId(params("collection_id"))
  //     (for {
  //       collection <- ArchCollection.get(collectionId)
  //     } yield {
  //       val jobs =
  //         JobManager.jobs.values.toSeq
  //           .filter(_.category != ArchJobCategories.None)
  //           .groupBy(_.category)
  //           .map {
  //             case (category, jobs) =>
  //               category -> jobs.sortBy(_.name.toLowerCase)
  //           }
  //       Ok(
  //         ssp(
  //           "jobs",
  //           "breadcrumbs" -> Seq(
  //             (relativePath("/" + collection.userUrlId + "/analysis"), collection.name),
  //             (relativePath("/" + collection.userUrlId + "/jobs"), "Generate Datasets")),
  //           "jobs" -> jobs,
  //           "user" -> context.user,
  //           "collection" -> collection),
  //         Map("Content-Type" -> "text/html"))
  //     }).getOrElse(NotFound())
  //   }
  // }

  get(ViewPathPatterns.Dataset) {
    ensureLogin { implicit context =>
      val datasetId = params("dataset_id")
      val sample = params.get("sample").contains("true")
      (for {
        (collection, job) <- DatasetUtil.parseId(datasetId, context.user);
        conf <- DerivationJobConf.collection(collection, sample)
        instance <- JobManager.getInstanceOrGlobal(
          job.id,
          conf,
          DerivationJobConf.collection(collection, sample, global = true)
        )
      } yield {
        instance.job.templateName match {
          case Some(templateName) =>
            val attributes = Seq(
              "breadcrumbs" -> Seq(
                BreadCrumbs.Datasets,
                BreadCrumbs.Dataset(datasetId, sample),
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

  // TODO - prune this route and associated template
  // get("/subset/?") {
  //   ensureLogin { implicit context =>
  //     val collectionId = ArchCollection.userCollectionId("UNION-UDQ")
  //     ArchCollection
  //       .get(collectionId)
  //       .map { collection =>
  //         Ok(
  //           ssp(
  //             "union-subset",
  //             "breadcrumbs" -> Seq((relativePath("/subset"), "Sub-Collection Query")),
  //             "user" -> context.user,
  //             "collection" -> collection,
  //             "collections" -> ArchCollection
  //               .userCollections(context.user)
  //               .map(_.sourceId)
  //               .distinct
  //               .sorted),
  //           Map("Content-Type" -> "text/html"))
  //       }
  //       .getOrElse(NotFound())
  //   }
  // }

  get(ViewPathPatterns.DatasetExplorer) {
    ensureLogin { implicit context =>
      Ok(
        ssp(
          "datasets-explore",
          "breadcrumbs" -> Seq(
            BreadCrumbs.Datasets,
            (ViewPathPatterns.reverse(ViewPathPatterns.DatasetExplorer), "Explore"),
          ),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html")
      )
    }
  }

  get(ViewPathPatterns.GenerateDataset) {
    ensureLogin { implicit context =>
      Ok(
        ssp(
          "datasets-generate",
          "breadcrumbs" -> Seq(
            BreadCrumbs.Datasets,
            (ViewPathPatterns.reverse(ViewPathPatterns.GenerateDataset), "Generate"),
          ),
          "user" -> context.user,
        ),
        Map("Content-Type" -> "text/html")
      )
    }
  }

  get(ViewPathPatterns.Login) {
    val next = Try(params("next")).toOption.filter(_ != null).getOrElse(ArchConf.baseUrl)
    Ok(
      ssp(
        "login",
        "breadcrumbs" -> Seq(
          BreadCrumbs.Login,
        ),
        "next" -> next
      ),
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
              "breadcrumbs" -> Seq(
                BreadCrumbs.Login,
              ),
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
    login(ArchConf.baseUrl)
  }
}

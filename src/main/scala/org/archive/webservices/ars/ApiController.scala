package org.archive.webservices.ars

import org.scalatra.{NotFound, NotImplemented, Ok}
import _root_.io.circe.syntax._
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager, ProcessingState}

class ApiController extends BaseController {
  get("/runjob/:jobid/:collectionid") {
    ensureLogin(redirect = false) { user =>
      val collectionId = params("collectionid")
      JobManager.get(params("jobid")) match {
        case Some(job) =>
          DerivationJobConf.collection(collectionId) match {
            case Some(conf) =>
              val instance = job.enqueue(conf).getOrElse(job.history(conf))
              Ok(Map(
                "state" -> instance.stateStr.asJson,
                "started" -> (instance.state != ProcessingState.NotStarted).asJson,
                "finished" -> (instance.state == ProcessingState.Finished).asJson
              ).asJson.spaces4, Map("Content-Type" -> "application/json"))
            case None =>
              NotImplemented()
          }
        case None =>
          NotFound()
      }
    }
  }

  get("/jobstate/:jobid/:collectionid") {
    ensureLogin(redirect = false) { user =>
      val jobId = params("jobid")
      val collectionId = params("collectionid")
      JobManager.getInstance(collectionId, jobId) match {
        case Some(instance) =>
          Ok(Map(
            "state" -> instance.stateStr.asJson,
            "started" -> (instance.state != ProcessingState.NotStarted).asJson,
            "finished" -> (instance.state == ProcessingState.Finished).asJson
          ).asJson.spaces4, Map("Content-Type" -> "application/json"))
        case None =>
          NotFound()
      }
    }
  }
}

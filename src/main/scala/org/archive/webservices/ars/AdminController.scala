package org.archive.webservices.ars

import java.io.{File, FileInputStream}

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import org.apache.commons.io.input.BoundedInputStream
import org.archive.webservices.ars.model.collections.{
  AitCollectionSpecifics,
  SpecialCollectionSpecifics
}
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.processing.JobStateManager
import org.archive.webservices.ars.processing.JobStateManager.Charset
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.util.DigestUtil
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

import scala.io.Source
import scala.util.Try

class AdminController extends BaseController with ScalateSupport {
  get("/?") {
    ensureLogin { implicit user =>
      if (user.isAdmin)
        Ok(
          ssp("admin", "user" -> user, "baseUrl" -> relativePath("")),
          Map("Content-Type" -> "text/html"))
      else Forbidden()
    }
  }

  private def renderEdit(user: ArchUser, message: Option[String] = None): ActionResult = {
    Ok(
      ssp(
        "admin-edit",
        "user" -> user,
        "usersJson" -> IOUtil.lines("data/arch-users.json").mkString("\n"),
        "aitUsersJson" -> IOUtil.lines("data/ait-users.json").mkString("\n"),
        "aitCollectionsJson" -> IOUtil.lines("data/ait-collections.json").mkString("\n"),
        "specialCollectionsJson" -> IOUtil
          .lines("data/special-collections.json")
          .mkString("\n"),
        "message" -> message),
      Map("Content-Type" -> "text/html"))
  }

  get("/edit") {
    ensureLogin { user =>
      if (user.isAdmin) renderEdit(user)
      else Forbidden()
    }
  }

  post("/edit") {
    ensureLogin { user =>
      if (user.isAdmin) {
        val r = for {
          usersJsonStr <- params.get("users-json")
          aitUsersJsonStr <- params.get("ait-users-json")
          aitCollectionsJsonStr <- params.get("ait-collections-json")
          specialCollectionsJsonStr <- params.get("special-collections-json")
        } yield {
          parse(usersJsonStr).right.flatMap { usersJ =>
            parse(aitUsersJsonStr).right.flatMap { aitUsersJ =>
              parse(aitCollectionsJsonStr).right.flatMap { aitJ =>
                parse(specialCollectionsJsonStr).right.map((usersJ, aitUsersJ, aitJ, _))
              }
            }
          } match {
            case Left(failure) =>
              renderEdit(user, Some(failure.getMessage))
            case Right((usersJson, aitUsersJson, aitCollectionsJson, specialCollectionsJson)) =>
              val usersCursor = usersJson.hcursor
              val usersJsonOut = usersCursor.keys.toIterator.flatten.flatMap { username =>
                val userMap = usersCursor.downField(username)
                userMap.keys.map { userFields =>
                  username -> userFields
                    .flatMap { field =>
                      (if (field == "password") {
                         userMap
                           .get[String](field)
                           .toOption
                           .map { pw =>
                             if (pw.startsWith("sha1:")) pw
                             else "sha1:" + DigestUtil.sha1Base32(pw)
                           }
                           .map(_.asJson)
                       } else userMap.downField(field).focus).map(field -> _)
                    }
                    .toMap
                    .asJson
                }
              }.toMap
              IOUtil.writeLines("data/arch-users.json", Seq(usersJsonOut.asJson.spaces4))
              IOUtil.writeLines("data/ait-users.json", Seq(aitUsersJson.spaces4))
              IOUtil.writeLines("data/ait-collections.json", Seq(aitCollectionsJson.spaces4))
              IOUtil.writeLines(
                "data/special-collections.json",
                Seq(specialCollectionsJson.spaces4))
              ArchUser.invalidateData()
              AitCollectionSpecifics.invalidateData()
              SpecialCollectionSpecifics.invalidateData()
              renderEdit(user)
          }
        }
        r.getOrElse(MethodNotAllowed())
      } else Forbidden()
    }
  }

  val MaxLogLength: Int = 1.mb.toInt
  get("/logs/:log_type") {
    ensureLogin { user =>
      if (user.isAdmin) {
        params("log_type") match {
          case "jobs" =>
            val tail = params.get("tail").flatMap(str => Try(str.toInt).toOption).getOrElse(-1)
            val logFile = new File(s"${JobStateManager.LoggingDir}/${JobStateManager.JobLogFile}")
            if (logFile.exists) {
              val skip = if (tail < 0) 0L else (logFile.length - tail.min(MaxLogLength)).max(0L)
              val in = new BoundedInputStream(new FileInputStream(logFile), MaxLogLength)
              try {
                IOUtil.skip(in, skip)
                val source = Source.fromInputStream(in, JobStateManager.Charset)
                try {
                  Ok(source.mkString, Map("Content-Type" -> "text/plain"))
                } finally {
                  source.close()
                }
              } finally {
                in.close()
              }
            } else NotFound()
          case "running" =>
            val runningJobsFile =
              new File(s"${JobStateManager.LoggingDir}/${JobStateManager.RunningJobsFile}")
            if (runningJobsFile.exists) {
              val source = Source.fromFile(runningJobsFile, Charset)
              try {
                Ok(source.mkString, Map("Content-Type" -> "application/json"))
              } finally {
                source.close()
              }
            } else NotFound()
          case "failed" =>
            val failedJobsFile =
              new File(s"${JobStateManager.LoggingDir}/${JobStateManager.FailedJobsFile}")
            if (failedJobsFile.exists) {
              val source = Source.fromFile(failedJobsFile, Charset)
              try {
                Ok(source.mkString, Map("Content-Type" -> "text/plain"))
              } finally {
                source.close()
              }
            } else NotFound()
          case _ =>
            NotFound()
        }
      } else Forbidden()
    }
  }
}

package org.archive.webservices.ars

import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.{ArchCollection, ArchConf, DerivativeOutput}
import org.archive.webservices.ars.processing.{DerivationJobConf, JobManager}
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.StringUtil
import org.scalatra._

import java.io.{File, PrintWriter}
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.servlet.http.HttpServletRequest
import scala.io.Source
import scala.util.{Random, Try}

class FilesController extends BaseController {
  private val NotebooksTemplatesDir = "templates/notebooks"
  private val GistIdPrefix = "ARCH_Colab_Notebook"
  private val NotebookDatasetAssignStr = "dataset = '"

  private def sendFile(file: DerivativeOutput)(
      implicit request: HttpServletRequest): ActionResult = {
    val size = file.size
    val rangeStrOpt = request
      .header("Range")
      .map(_.trim)
      .filter(_.startsWith("bytes="))
      .map(_.stripPrefix("bytes=").split(',').head.trim)
      .filter(_.nonEmpty)
    val range = rangeStrOpt
      .map { rangeStr =>
        if (rangeStr.startsWith("-")) {
          val len = Try(rangeStr.stripPrefix("-").toLong).toOption.getOrElse(0L)
          (size - len, size - 1)
        } else {
          val split = rangeStr.split('-')
          val from = split.headOption.flatMap(str => Try(str.toLong).toOption).getOrElse(0L)
          val to = split.drop(1).headOption.flatMap(str => Try(str.toLong).toOption).getOrElse(0L)
          (from.max(0L), to.min(size - 1))
        }
      }
      .filter {
        case (from, to) => from >= 0 && to < size && to >= from && (to - from + 1) < size
      }
    val (offset, length, status) =
      range.map { case (from, to) => (from, to - from + 1, 206) }.getOrElse((0L, size, 200))
    ActionResult(
      ResponseStatus(status),
      HdfsIO.open(
        file.path,
        offset = offset,
        length = length,
        decompress = false,
        strategy = HdfsIO.LoadingStrategy.Remote),
      Map(
        "Content-Type" -> file.mimeType,
        "Accept-Ranges" -> "bytes",
        "Content-Disposition" -> ("attachment; filename=" + file.filename),
        "Content-Length" -> length.toString) ++ range.map {
        case (from, to) =>
          "Content-Range" -> s"bytes $from-$to/$size"
      })
  }

  get("/download/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val sample = params.get("sample").contains("true")
    val filename = params("file_name")
    params.get("access") match {
      case Some(accessToken) =>
        val jobId = params("job_id")
        (for {
          collection <- ArchCollection.get(collectionId)
          conf <- DerivationJobConf.collection(collection, sample = sample)
          instance <- JobManager.getInstanceOrGlobal(jobId, conf, DerivationJobConf.collection(collection, sample = sample, global = true))
        } yield {
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) =>
              if (file.accessToken == accessToken) {
                sendFile(file)
              } else Forbidden()
            case None =>
              NotFound()
          }
        }).getOrElse(NotFound())
      case None =>
        ensureLogin(redirect = false, useSession = true) { implicit user =>
            val jobId = params("job_id")
            (for {
              collection <- ArchCollection.get(collectionId)
              conf <- DerivationJobConf.collection(collection, sample = sample)
              instance <- JobManager.getInstanceOrGlobal(jobId, conf, DerivationJobConf.collection(collection, sample = sample, global = true))
            } yield {
              instance.outFiles.find(_.filename == filename) match {
                case Some(file) =>
                  sendFile(file)
                case None =>
                  NotFound()
              }
            }).getOrElse(NotFound())
        }
    }
  }

  get("/preview/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val sample = params.get("sample").contains("true")
    val filename = params("file_name")
    ensureLogin(redirect = false, useSession = true) { implicit context =>
        val jobId = params("job_id")
        (for {
          collection <- ArchCollection.get(collectionId)
          conf <- DerivationJobConf.collection(collection, sample = sample)
          instance <- JobManager.getInstanceOrGlobal(jobId, conf, DerivationJobConf.collection(collection, sample = sample, global = true))
        } yield {
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) =>
              Ok(
                HdfsIO.lines(file.path, n = 51).mkString("\n"),
                Map(
                  "Content-Type" -> file.mimeType,
                  "Content-Disposition" -> ("attachment; filename=" + file.filename.stripSuffix(Sparkling.GzipExt))))
            case None =>
              NotFound()
          }
        }).getOrElse(NotFound())
    }
  }

  def gistApiRequest(
      postBody: Option[String] = None,
      delete: Option[String] = None): Option[HCursor] = ArchConf.githubBearer.flatMap {
    githubBearer =>
      val ait =
        new URL("https://api.github.com/gists" + delete.map("/" + _).getOrElse("")).openConnection
          .asInstanceOf[HttpsURLConnection]
      try {
        ait.setRequestProperty("Accept", "application/vnd.github+json")
        ait.setRequestProperty("Authorization", "Bearer " + githubBearer)
        ait.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        if (delete.isDefined) {
          ait.setRequestMethod("DELETE")
          ait.connect()
          None
        } else {
          ait.setDoInput(true)
          if (postBody.isDefined) {
            ait.setRequestMethod("POST")
            ait.setDoOutput(true)
            val out = ait.getOutputStream
            try {
              val writer = new PrintWriter(out)
              writer.write(postBody.get)
              writer.flush()
              writer.close()
            } finally {
              Try(out.close())
            }
          }
          ait.connect()
          if (ait.getResponseCode / 100 == 2) {
            val source = Source.fromInputStream(ait.getInputStream, "utf-8")
            val content = try source.mkString finally source.close()
            parse(content).toOption.map(_.hcursor)
          } else None
        }
      } finally {
        Try(ait.disconnect())
      }
  }

  def cleanGists(currentIdPrefix: String): Unit = {
    for (cursor <- gistApiRequest()) {
      for (gist <- cursor.values.toIterator.flatten.map(_.hcursor)) {
        if (gist
              .get[String]("description")
              .toOption
              .filter(_.startsWith(GistIdPrefix))
              .exists(!_.startsWith(currentIdPrefix))) {
          for (id <- gist.get[String]("id").toOption) gistApiRequest(delete = Some(id))
        }
      }
    }
  }

  get("/colab/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val sample = params.get("sample").contains("true")
    val filename = params("file_name")
    params.get("access") match {
      case Some(accessToken) =>
        val jobId = params("job_id")
        (for {
          collection <- ArchCollection.get(collectionId)
          conf <- DerivationJobConf.collection(collection, sample = sample)
          instance <- JobManager.getInstanceOrGlobal(jobId, conf, DerivationJobConf.collection(collection, sample = sample, global = true))
        } yield {
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) =>
              if (file.accessToken == accessToken) {
                val notebookName = StringUtil.prefixBySeparator(filename, ".") + ".ipynb"
                val notebookTemplate = new File(NotebooksTemplatesDir + "/" + notebookName)
                if (notebookTemplate.exists) {
                  val source = Source.fromFile(notebookTemplate)
                  val contentTemplate = try source.mkString finally source.close()
                  val datasetAssignIdx = contentTemplate.indexOf(NotebookDatasetAssignStr)
                  val content =
                    if (datasetAssignIdx < 0) contentTemplate
                    else {
                      val datasetAssignStart = datasetAssignIdx + NotebookDatasetAssignStr.length
                      val datasetAssignEnd = contentTemplate.indexOf("'", datasetAssignStart)
                      val datasetAssignVal =
                        contentTemplate.substring(datasetAssignStart, datasetAssignEnd)
                      contentTemplate.replace(
                        datasetAssignVal,
                        s"https://webdata.archive-it.org/ait/files/download/$collectionId/$jobId/$filename?access=" + file.accessToken)
                    }
                  val nowStr = java.time.Instant.now.toString
                  val dateStr = StringUtil.prefixBySeparator(nowStr, "T")
                  cleanGists(GistIdPrefix + " " + dateStr)
                  val gistId = Seq(
                    GistIdPrefix,
                    nowStr,
                    collectionId + "/" + jobId + "?sample=" + sample,
                    filename,
                    Random.nextString(10)).mkString(" ")
                  val postBody = Map(
                    "description" -> gistId.asJson,
                    "public" -> false.asJson,
                    "files" -> Map(notebookName -> Map("content" -> content)).asJson).asJson
                    .noSpaces
                  (for {
                    cursor <- gistApiRequest(Some(postBody))
                    id <- cursor.get[String]("id").toOption
                    owner <- cursor.downField("owner").get[String]("login").toOption
                  } yield {
                    val colabUrl = "http://colab.research.google.com/gist/" + owner + "/" + id + "/" + notebookName
                    Found(colabUrl)
                  }).getOrElse(InternalServerError())
                } else NotFound("No notebook for this file found!")
              } else Forbidden()
            case None =>
              NotFound()
          }
        }).getOrElse(NotFound())
      case None =>
        Forbidden()
    }
  }
}

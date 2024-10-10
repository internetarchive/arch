package org.archive.webservices.ars

import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.collections.AitCollectionSpecifics
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.model.{ArchCollection, ArchConf, DerivativeOutput}
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobInstance}
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.StringUtil
import org.scalatra._

import java.io.{File, PrintWriter}
import java.net.URL
import java.time.Instant
import javax.net.ssl.HttpsURLConnection
import javax.servlet.http.HttpServletRequest
import scala.io.Source
import scala.util.Try

object FilesController {
  private val NotebooksTemplatesDir = "templates/notebooks"
  private val GistIdPrefix = "ARCH_Colab_Notebook"

  def sendFile(file: DerivativeOutput)(implicit request: HttpServletRequest): ActionResult = {
    val size = file.size
    val rangeStrOpt = Option(request.getHeader("Range"))
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
      .filter { case (from, to) =>
        from >= 0 && to < size && to >= from && (to - from + 1) < size
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
        "Content-Disposition" -> ("attachment; filename=" + file.downloadName),
        "Content-Length" -> length.toString) ++ range.map { case (from, to) =>
        "Content-Range" -> s"bytes $from-$to/$size"
      })
  }

  def preview(instance: DerivationJobInstance, filename: String): ActionResult = {
    instance.outFiles.find(_.filename == filename) match {
      case Some(file) =>
        Ok(
          HdfsIO.lines(file.path, n = 51).mkString("\n"),
          Map(
            "Content-Type" -> file.mimeType,
            "Content-Disposition" -> ("attachment; filename=" + file.filename.stripSuffix(
              Sparkling.GzipExt))))
      case None =>
        NotFound()
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
            val content =
              try source.mkString
              finally source.close()
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

  def colab(
      instance: DerivationJobInstance,
      filename: String,
      fileUrl: String,
      accessToken: String): ActionResult = {
    instance.outFiles.find(_.filename == filename) match {
      case Some(file) =>
        if (file.accessToken == accessToken) {
          val prefix = StringUtil.prefixBySeparator(filename, ".")
          val notebookFile = prefix + ".ipynb"
          val notebookFilename = prefix + "-" + instance.uuid + ".ipynb"
          val notebookTemplate = new File(NotebooksTemplatesDir + "/" + notebookFile)
          if (notebookTemplate.exists) {
            val source = Source.fromFile(notebookTemplate)
            val contentTemplate =
              try source.mkString
              finally source.close()
            var content = contentTemplate.replace("ARCHDATASETURL", fileUrl)
            val inputSpec = instance.conf.inputSpec
            if (InputSpec.isCollectionBased(inputSpec) && inputSpec.collectionId.startsWith(
                AitCollectionSpecifics.Prefix)) {
              val waybackUrl = ArchConf.waybackBaseUrl + "/" + AitCollectionSpecifics.getAitId(
                inputSpec.collection)
              content = content.replace("ARCHCOLLECTIONIDURL", waybackUrl)
            }
            val nowStr = java.time.Instant.now.toString
            val dateStr = StringUtil.prefixBySeparator(nowStr, "T")
            cleanGists(GistIdPrefix + " " + dateStr)
            val gistId = Seq(
              GistIdPrefix,
              nowStr,
              instance.uuid,
              filename,
              Instant.now.toEpochMilli.toString)
              .mkString(" ")
            val postBody = Map(
              "description" -> gistId.asJson,
              "public" -> false.asJson,
              "files" -> Map(
                notebookFilename -> Map("content" -> content)).asJson).asJson.noSpaces
            (for {
              cursor <- gistApiRequest(Some(postBody))
              id <- cursor.get[String]("id").toOption
              owner <- cursor.downField("owner").get[String]("login").toOption
            } yield {
              val colabUrl =
                "http://colab.research.google.com/gist/" + owner + "/" + id + "/" + notebookFilename
              Found(colabUrl)
            }).getOrElse(InternalServerError())
          } else NotFound("No notebook for this file found!")
        } else Forbidden()
      case None =>
        NotFound()
    }
  }
}

class FilesController extends BaseController {
  get("/download/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val jobId = params("job_id")
    val filename = params("file_name")
    val sample = params.get("sample").contains("true")
    params.get("access") match {
      case Some(accessToken) =>
        (for {
          collection <- ArchCollection.get(collectionId)
          instance <- DerivationJobConf.collectionInstance(jobId, collection, sample)
        } yield {
          instance.outFiles.find(_.filename == filename) match {
            case Some(file) =>
              if (file.accessToken == accessToken) {
                FilesController.sendFile(file.prefixDownload(instance))
              } else Forbidden()
            case None =>
              NotFound()
          }
        }).getOrElse(NotFound())
      case None =>
        ensureAuth { implicit context =>
          (for {
            collection <- ArchCollection.get(
              ArchCollection.userCollectionId(collectionId, context.user))
            instance <- DerivationJobConf.collectionInstance(jobId, collection, sample)
          } yield {
            instance.outFiles.find(_.filename == filename) match {
              case Some(file) =>
                FilesController.sendFile(file.prefixDownload(instance))
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
    ensureAuth { implicit context =>
      val jobId = params("job_id")
      (for {
        collection <- ArchCollection.get(
          ArchCollection.userCollectionId(collectionId, context.user))
        instance <- DerivationJobConf.collectionInstance(jobId, collection, sample)
      } yield FilesController.preview(instance, filename)).getOrElse(NotFound())
    }
  }

  get("/colab/:collection_id/:job_id/:file_name") {
    val collectionId = params("collection_id")
    val jobId = params("job_id")
    val filename = params("file_name")
    val sample = params.get("sample").contains("true")
    params.get("access") match {
      case Some(accessToken) => {
        val fileUrl = params
          .get("file_download_url")
          .getOrElse(
            s"${ArchConf.baseUrl}/files/download/$collectionId/$jobId/$filename?sample=${sample}&access=${accessToken}")
        (for {
          collection <- ArchCollection.get(collectionId)
          instance <- DerivationJobConf.collectionInstance(jobId, collection, sample)
        } yield {
          FilesController.colab(instance, filename, fileUrl, accessToken)
        }).getOrElse(NotFound())
      }
      case None =>
        Forbidden()
    }
  }
}

package org.archive.webservices.ars.model

import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax._
import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.WebPagesExtraction
import org.archive.webservices.ars.processing.jobs.system.{DatasetPublication, UserDefinedQuery}
import org.archive.webservices.ars.util.{Common, HttpUtil}
import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil}
import org.archive.webservices.sparkling.util.DigestUtil

import java.io.InputStream
import java.net.{HttpURLConnection, URL}
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.annotation.tailrec
import scala.collection.immutable.{ListMap, Map}
import scala.io.Source
import scala.util.Try

object PublishedDatasets {
  val NAAN = 13960
  val MetadataFields: Set[String] =
    Set("creator", "description", "licenseurl", "subject", "title")
  val DeleteCommentPrefix = "ARCH:"

  val ProhibitedJobs: Set[DerivationJob] =
    Set(DatasetPublication, UserDefinedQuery, WebPagesExtraction)

  private var sync = Set.empty[String]

  def jobFile(instance: DerivationJobInstance): String = {
    instance.outPath + "/published.json"
  }

  def collectionFile(collection: ArchCollection): String = {
    collectionFile(DerivationJobConf.collectionOutPath(collection))
  }

  def collectionFile(collectionOutPath: String): String = collectionOutPath + "/published.json"

  def isCollectionBased(instance: DerivationJobInstance): Boolean = {
    InputSpec.isCollectionBased(instance.conf.inputSpec) && {
      val collectionPath = DerivationJobConf.collectionOutPath(instance.conf.inputSpec.collection)
      instance.outPath.startsWith(collectionPath + "/")
    }
  }

  val MaxInputIdLength = 25
  def itemName(instance: DerivationJobInstance): String = {
    val jobId = instance.job.id + (if (instance.conf.isSample) "-sample" else "")
    val inputSpecId = instance.conf.inputSpec.id
    val inputId =
      if (inputSpecId.length <= MaxInputIdLength) inputSpecId
      else {
        val prefix = ArchCollection.prefix(inputSpecId).getOrElse {
          inputSpecId.split("[\\W_]", 2).head.take(10) + "-"
        }
        prefix + DigestUtil.md5Hex(inputSpecId).take(16)
      }
    val timestamp = instance.info.started
      .getOrElse(Instant.now)
      .truncatedTo(ChronoUnit.SECONDS)
      .toString
      .replaceAll("[^\\dTZ]", "")
    Seq("arch", inputId, jobId, timestamp).mkString("_").replaceAll("[^a-zA-Z0-9_.-]", "-")
  }

  def syncCollectionFile[R](f: String)(action: => R): R = {
    while (sync.contains(f) || synchronized {
        sync.contains(f) || {
          sync += f
          false
        }
      }) {}
    try {
      action
    } finally {
      synchronized(sync -= f)
    }
  }

  case class ItemInfo(
      item: String,
      inputId: String,
      job: String,
      sample: Boolean,
      time: Instant,
      complete: Boolean,
      ark: Option[String]) {
    def toJson(includeItem: Boolean): Json = {
      (if (includeItem) ListMap("item" -> item.asJson) else ListMap.empty) ++ Map(
        "inputId" -> inputId.asJson,
        "job" -> job.asJson,
        "sample" -> sample.asJson,
        "time" -> time.toString.asJson,
        "complete" -> complete.asJson) ++ ark.map("ark" -> _.asJson).toMap
    }.asJson
  }

  def newItemInfo(itemName: String, instance: DerivationJobInstance): ItemInfo = {
    ItemInfo(
      itemName,
      instance.conf.inputSpec.id,
      instance.job.id,
      instance.conf.isSample,
      Instant.now,
      complete = false,
      ark(itemName))
  }

  def appendCollectionFile(collection: ArchCollection, info: ItemInfo): Unit = {
    val collectionOutPath = DerivationJobConf.collectionOutPath(collection)
    HdfsIO.fs.mkdirs(new Path(collectionOutPath))
    val f = collectionFile(collectionOutPath)
    syncCollectionFile(f) {
      val in = if (HdfsIO.exists(f)) {
        parse(HdfsIO.lines(f).mkString).toSeq
          .map(_.hcursor)
          .flatMap { cursor =>
            cursor.keys.toSeq.flatten.flatMap(key => cursor.downField(key).focus.map(key -> _))
          }
          .toMap
      } else Map.empty[String, Json]
      HdfsIO.writeLines(
        f,
        Seq(in.updated(info.item, info.toJson(includeItem = false)).asJson.spaces4),
        overwrite = true)
    }
  }

  def collectionItems(collection: ArchCollection): Set[String] = {
    val collectionFilePath = collectionFile(DerivationJobConf.collectionOutPath(collection))
    parse(HdfsIO.lines(collectionFilePath).mkString).toSeq
      .map(_.hcursor)
      .flatMap { cursor =>
        cursor.keys.toSeq.flatten
      }
      .toSet
  }

  def collectionInfoJson(collection: ArchCollection): String = {
    val collectionFilePath = collectionFile(DerivationJobConf.collectionOutPath(collection))
    HdfsIO.lines(collectionFilePath).mkString("\n")
  }

  def request(
      path: String,
      s3: Boolean = false,
      update: Boolean = false,
      metadata: Map[String, Seq[String]] = Map.empty,
      put: Option[(InputStream, Long)] = None,
      post: Option[String] = None): Either[(Int, String), String] = ArchConf.iaAuthHeader
    .map { iaAuthHeader =>
      val url = (if (s3) ArchConf.pboxS3Url else ArchConf.iaBaseUrl) + "/" + path
        .stripPrefix("/")
      val connection = HttpUtil.openConnection(url)
      try {
        connection.setRequestProperty("Authorization", iaAuthHeader)
        connection.setDoInput(true)
        if (s3) {
          for ((key, value) <- metadata) {
            if (value.size == 1)
              connection.setRequestProperty("x-archive-meta-" + key, value.head)
            else {
              for ((v, i) <- value.zipWithIndex) {
                val metaIdx = (if (i < 9) "0" else "") + (i + 1)
                connection.setRequestProperty(s"x-archive-meta$metaIdx-" + key, v)
              }
            }
          }
          if (update) connection.setRequestProperty("x-archive-ignore-preexisting-bucket", "1")
        }
        if (s3 || put.isDefined) {
          connection.setRequestMethod("PUT")
          for ((in, length) <- put) {
            connection.setDoOutput(true)
            connection.setFixedLengthStreamingMode(length)
            val out = connection.getOutputStream
            try {
              IOUtil.copy(in, out, length)
            } finally {
              Try(out.close())
            }
          }
        } else if (post.isDefined) {
          connection.setRequestMethod("POST")
          for (postBody <- post) {
            connection.setDoOutput(true)
            val out = IOUtil.print(connection.getOutputStream, closing = true)
            try {
              out.println(postBody)
            } finally {
              Try(out.close())
            }
          }
        }
        connection.connect()
        val responseCode = connection.getResponseCode
        if (responseCode / 100 == 2) {
          val source = Source.fromInputStream(connection.getInputStream, "utf-8")
          try Right(source.mkString)
          finally source.close()
        } else if (responseCode / 100 >= 4) {
          // Report the request error.
          val source = Source.fromInputStream(connection.getErrorStream, "utf-8")
          try {
            val error = source.mkString
            Arch.reportError(
              "PublishedDatasets Petabox Response Error",
              error,
              Map(
                "status_code" -> responseCode.toString,
                "path" -> path,
                "method" -> connection.getRequestMethod))
            Left(responseCode, error)
          } finally source.close()
        } else Left(-1, "")
      } finally {
        Try(connection.disconnect())
      }
    }
    .getOrElse(Left(-1, ""))

  def ark(itemName: String): Option[String] = ArchConf.arkMintBearer.flatMap { arkMintBearer =>
    val connection = HttpUtil.openConnection(ArchConf.arkMintUrl)
    try {
      connection.setRequestMethod("POST")
      connection.setRequestProperty("Authorization", "Bearer " + arkMintBearer)
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setDoInput(true)
      connection.setDoOutput(true)
      val out = IOUtil.print(connection.getOutputStream, closing = true)
      try {
        out.println(
          Map(
            "naan" -> NAAN.asJson,
            "shoulder" -> "/a3".asJson,
            "url" -> (ArchConf.iaBaseUrl + "/details/" + itemName).asJson,
            "metadata" -> Map.empty[String, String].asJson,
            "maintenance_commitment" -> Map.empty[String, String].asJson).asJson.noSpaces)
      } finally {
        out.close()
      }
      connection.connect()
      if (connection.getResponseCode / 100 == 2) {
        val source = Source.fromInputStream(connection.getInputStream, "utf-8")
        try {
          parse(source.mkString).toOption.flatMap(_.hcursor.get[String]("ark").toOption)
        } finally source.close()
      } else None
    } finally {
      Try(connection.disconnect())
    }
  }

  def dataset(jobId: String, conf: DerivationJobConf): Option[DerivationJobInstance] = {
    DerivationJobConf
      .collectionInstance(jobId, conf)
      .filter(_.state == ProcessingState.Finished)
  }

  def dataset(
      jobId: String,
      collection: ArchCollection,
      sample: Boolean): Option[DerivationJobInstance] = {
    DerivationJobConf
      .collectionInstance(jobId, collection, sample)
      .filter(_.state == ProcessingState.Finished)
  }

  def publish(
      jobId: String,
      conf: DerivationJobConf,
      metadata: Map[String, Seq[String]]): Option[ItemInfo] = {
    for (instance <- dataset(jobId, conf)) yield publish(instance, metadata)
  }.flatten

  def publish(
      dataset: DerivationJobInstance,
      metadata: Map[String, Seq[String]]): Option[ItemInfo] = {
    val jobFilePath = jobFile(dataset)
    val itemInfoOpt = jobItem(jobFilePath)
    itemInfoOpt match {
      case Some(itemInfo) =>
        updateItem(itemInfo.item, metadata)
        itemInfoOpt
      case None =>
        HdfsIO.fs.createNewFile(new Path(jobFilePath))
        val name = itemName(dataset)
        Try(newItemInfo(name, dataset))
          .filter { itemInfo =>
            Try {
              createItem(name, datasetMetadata(dataset, itemInfo) ++ metadata) && {
                if (isCollectionBased(dataset)) {
                  appendCollectionFile(dataset.conf.inputSpec.collection, itemInfo)
                }
                HdfsIO.writeLines(
                  jobFilePath,
                  Seq(itemInfo.toJson(includeItem = true).spaces4),
                  overwrite = true) > 0
              }
            }.getOrElse(false)
          }
          .toOption
          .orElse {
            HdfsIO.delete(jobFilePath)
            throw new RuntimeException(s"Creating new Petabox item $name failed.")
          }
    }
  }

  def datasetMetadata(
      instance: DerivationJobInstance,
      info: ItemInfo): Map[String, Seq[String]] = {
    Map("collection" -> Seq(ArchConf.pboxCollection)) ++ info.ark.map("Identifier-ark" -> Seq(_))
  }

  def jobItem(dataset: DerivationJobInstance): Option[ItemInfo] = {
    jobItem(PublishedDatasets.jobFile(dataset))
  }

  def jobItem(jobFile: String): Option[ItemInfo] = {
    if (HdfsIO.exists(jobFile)) {
      parse(HdfsIO.lines(jobFile).mkString).toOption.map(_.hcursor).flatMap { cursor =>
        for {
          item <- cursor.get[String]("item").toOption
          inputId <- cursor.get[String]("inputId").toOption.orElse {
            cursor.get[String]("collection").toOption
          }
          jobId <- cursor.get[String]("job").toOption
          isSample <- cursor.get[Boolean]("sample").toOption
          time <- cursor.get[String]("time").toOption.map(Instant.parse)
          complete <- cursor.get[Boolean]("complete").toOption
        } yield ItemInfo(
          item,
          inputId,
          jobId,
          isSample,
          time,
          complete,
          cursor.get[String]("ark").toOption)
      }
    } else None
  }

  def complete(instance: DerivationJobInstance, itemName: String): Boolean = {
    val jobFilePath = jobFile(instance)
    jobItem(jobFilePath).filter(_.item == itemName) match {
      case Some(itemInfo) =>
        HdfsIO.writeLines(
          jobFilePath,
          Seq(itemInfo.copy(complete = true).toJson(includeItem = true).spaces4),
          overwrite = true)
        if (isCollectionBased(instance)) {
          val collectionFilePath = collectionFile(instance.conf.inputSpec.collection)
          syncCollectionFile(collectionFilePath) {
            val in = parse(HdfsIO.lines(collectionFilePath).mkString).toOption
              .flatMap(_.as[Map[String, Json]].toOption)
              .getOrElse(Map.empty)
            val out =
              in.updated(itemName, itemInfo.copy(complete = true).toJson(includeItem = false))
            HdfsIO.writeLines(collectionFilePath, Seq(out.asJson.spaces4), overwrite = true)
          }
        }
        true
      case None => false
    }
  }

  private def isDark(name: String): Option[Boolean] = {
    request("services/tasks.php?history=1&identifier=" + name).toOption.map { tasks =>
      parse(tasks).toOption
        .map(_.hcursor)
        .flatMap { cursor =>
          cursor.downField("value").downField("history").downArray.focus.map(_.hcursor)
        }
        .exists { lastTask =>
          lastTask.get[String]("cmd").toOption.contains("make_dark.php") && {
            lastTask
              .downField("args")
              .get[String]("curation")
              .toOption
              .exists(_.contains("[comment]" + DeleteCommentPrefix))
          }
        }
    }
  }

  def createItem(name: String, metadata: Map[String, Seq[String]]): Boolean = {
    request(name, s3 = true, metadata = metadata).isRight || {
      isDark(name).contains(true) && {
        request(
          "/services/tasks.php",
          post = Some {
            ListMap(
              "cmd" -> "make_undark.php".asJson,
              "identifier" -> name.asJson,
              "args" -> Map(
                "comment" -> s"$DeleteCommentPrefix Re-publish item").asJson).asJson.noSpaces
          }).isRight && {
          {
            Common.retryWhile(
              isDark(name).getOrElse(true),
              sleepMs = 500,
              maxTimes = 12,
              _ * 2) && {
              request(name, s3 = true, update = true, metadata = metadata).isRight
            }
          } || {
            if (!Common.retryWhile(!deleteItem(name), sleepMs = 100, maxTimes = 10, _ * 2)) {
              Arch.reportWarning(
                "PublishedDatasets Create/Undark Item Error",
                "An Item is likely to be undarked, but not in use, please double check.",
                Map("item" -> name))
            }
            false
          }
        }
      }
    }
  }

  def updateItem(instance: DerivationJobInstance, metadata: Map[String, Seq[String]]): Boolean = {
    val jobFilePath = jobFile(instance)
    jobItem(jobFilePath)
      .map { info =>
        updateItem(info.item, metadata)
      }
      .getOrElse(false)
  }

  def updateItem(name: String, metadata: Map[String, Seq[String]]): Boolean = {
    this
      .metadata(name, all = true)
      .flatMap { existingMetadata =>
        request(name, s3 = true, update = true, metadata = existingMetadata ++ metadata).toOption
      }
      .isDefined
  }

  def deleteItem(name: String): Boolean = {
    request(
      "/services/tasks.php",
      post = Some {
        ListMap(
          "cmd" -> "make_dark.php".asJson,
          "identifier" -> name.asJson,
          "args" -> Map(
            "comment" -> s"$DeleteCommentPrefix Delete published item").asJson).asJson.noSpaces
      }).isRight
  }

  def deletePublished(collection: ArchCollection, itemName: String): Boolean = {
    val collectionFilePath = collectionFile(DerivationJobConf.collectionOutPath(collection))
    syncCollectionFile(collectionFilePath) {
      val in = parse(HdfsIO.lines(collectionFilePath).mkString).toOption
        .flatMap(_.as[Map[String, Json]].toOption)
        .getOrElse(Map.empty)
      for (itemInfo <- in.get(itemName).map(_.hcursor)) yield {
        if (deleteItem(itemName)) {
          for {
            jobId <- itemInfo.get[String]("job").toOption
            sample <- itemInfo.get[Boolean]("sample").toOption.orElse(Some(false))
            instance <- DerivationJobConf.collectionInstance(jobId, collection, sample)
          } {
            val jobFilePath = jobFile(instance)
            if (jobItem(jobFilePath).exists(_.item == itemName)) HdfsIO.delete(jobFilePath)
          }
          val out = in - itemName
          HdfsIO.writeLines(collectionFilePath, Seq(out.asJson.spaces4), overwrite = true)
          true
        } else false
      }
    }.getOrElse(false)
  }

  def deletePublished(instance: DerivationJobInstance): Boolean = {
    val jobFilePath = jobFile(instance)
    for (info <- jobItem(jobFilePath)) yield {
      if (isCollectionBased(instance)) {
        deletePublished(instance.conf.inputSpec.collection, info.item)
      } else {
        val success = deleteItem(info.item)
        if (success) HdfsIO.delete(jobFilePath)
        success
      }
    }
  }.getOrElse(false)

  def files(itemName: String): Set[String] = {
    request(s"metadata/$itemName/files").toOption
      .flatMap(parse(_).toOption.map(_.hcursor))
      .toSeq
      .flatMap { cursor =>
        cursor.downField("result").values.toSeq.flatten.map(_.hcursor).flatMap {
          _.get[String]("name").toOption
        }
      }
      .toSet
  }

  def metadata(
      instance: DerivationJobInstance,
      all: Boolean = false): Option[Map[String, Seq[String]]] = {
    val jobFilePath = jobFile(instance)
    jobItem(jobFilePath).flatMap(info => metadata(info.item, all))
  }

  def metadata(itemName: String): Option[Map[String, Seq[String]]] =
    metadata(itemName, all = false)

  def metadata(itemName: String, all: Boolean): Option[Map[String, Seq[String]]] = {
    request(s"metadata/$itemName/metadata").toOption
      .flatMap(parse(_).toOption)
      .flatMap(_.hcursor.downField("result").focus)
      .map { json =>
        val metadata = parseJsonMetadata(json)
        if (all) metadata else metadata.filterKeys(MetadataFields.contains)
      }
  }

  @tailrec
  def upload(itemName: String, filename: String, hdfsPath: String): Option[String] = {
    val fileSize = HdfsIO.length(hdfsPath)
    HdfsIO.access(hdfsPath, decompress = false) { in =>
      request(itemName + "/" + filename, s3 = true, put = Some((in, fileSize)))
    } match {
      case Right(_) => None
      case Left((responseCode, error)) =>
        if (responseCode == 503) {
          Thread.sleep(1000 * 60) // wait a minute
          upload(itemName, filename, hdfsPath)
        } else Some(s"$responseCode ($error)")
    }
  }

  def validateMetadata(metadata: Map[String, Seq[String]]): Option[String] = {
    metadata.keys.find(!MetadataFields.contains(_)).map { field =>
      s"Setting metadata field $field is not permitted."
    }
  }

  def parseJsonMetadata(json: Json): Map[String, Seq[String]] = {
    val cursor = json.hcursor
    cursor.keys.toSet.flatten.map { key =>
      val field = cursor.downField(key)
      val values = field.values.toSeq.flatten.flatMap(_.asString)
      key -> (if (values.isEmpty) field.focus.toSeq.flatMap(_.asString) else values)
    }.toMap
  }
}

package org.archive.webservices.ars.processing

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Json
import org.apache.hadoop.util.ShutdownHookManager
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.util.{DatasetUtil, FormatUtil, MailUtil}
import org.archive.webservices.ars.{Arch, Keystone, ViewPathPatterns}
import org.archive.webservices.sparkling.io.IOUtil

import java.io.{File, FileOutputStream, PrintStream}
import java.time.Instant
import scala.collection.immutable.ListMap
import scala.io.Source
import scala.util.Try

object JobStateManager {
  val LoggingDir = ArchConf.jobLoggingPath
  val JobLogFile = "jobs.log"
  val RunningJobsFile = "running.json"
  val FailedJobsFile = "failed.lst"
  val MetaSeparator = "--"
  val Charset = "utf-8"

  private var initialized = false
  private var running = ListMap.empty[String, Map[String, Json]]
  private var failed = ListMap.empty[String, String]

  private def access[R](action: => R): R = synchronized {
    init()
    action
  }

  private def saveRunning(): Unit = access {
    IOUtil.writeLines(s"$LoggingDir/$RunningJobsFile", Iterator(running.asJson.spaces4))
  }

  private def metaInfo(instance: DerivationJobInstance): Seq[(String, Json)] = {
    Seq(
      "uuid" -> instance.uuid.asJson,
      "attempt" -> instance.attempt.asJson,
      "slots" -> instance.slots.asJson,
      "size" -> FormatUtil.formatBytes(instance.inputSize).asJson) ++ {
      instance.user.map("user" -> _.id.asJson)
    }
  }

  private def registerRunning(instance: DerivationJobInstance): Unit = access {
    unregisterFailed(instance)
    running = running.updated(
      key(instance),
      ListMap(
        "id" -> instance.job.id.asJson,
        "conf" -> instance.conf.toJson,
        "state" -> instance.stateStr.asJson) ++ metaInfo(instance) ++ {
        val active = instance.active
        Seq("activeStage" -> active.job.stage.asJson) ++ active.queue
          .map("queue" -> _.name.asJson)
          .toSeq
      })
    saveRunning()
  }

  private def unregisterRunning(instance: DerivationJobInstance): Unit = access {
    val prevLength = running.size
    running -= key(instance)
    if (running.size != prevLength) saveRunning()
  }

  def updateRunning(instance: DerivationJobInstance): Unit = access {
    if (running.contains(key(instance))) registerRunning(instance)
  }

  private def saveFailed(): Unit = access {
    IOUtil.writeLines(s"$LoggingDir/$FailedJobsFile", failed.values)
  }

  private def registerFailed(instance: DerivationJobInstance): Unit = access {
    unregisterRunning(instance)
    failed = failed.updated(key(instance), str(instance))
    saveFailed()
  }

  private def unregisterFailed(instance: DerivationJobInstance): Unit = access {
    val prevLength = failed.size
    failed -= key(instance)
    if (failed.size != prevLength) saveFailed()
  }

  def rerunFailed(): Unit = {
    val jobs = failed.values.flatMap { str =>
      val indexOfMeta = str.lastIndexOf(MetaSeparator)
      val split = (if (indexOfMeta < 0) str else str.take(indexOfMeta)).split(" ", 3)
      DerivationJobConf.deserialize(split(2).trim).map { conf =>
        val jobId = split(0).trim
        val meta =
          if (indexOfMeta < 0) Json.Null
          else parse(str.drop(indexOfMeta + MetaSeparator.length).trim).getOrElse(Json.Null)
        (jobId, conf, meta.hcursor)
      }
    }.toList
    for {
      (id, conf, meta) <- jobs
      job <- JobManager.get(id)
    } {
      job.reset(conf)
      job.enqueue(
        conf,
        { instance =>
          instance.user = meta.downField("user").focus.flatMap(_.asString).flatMap(ArchUser.get)
          instance.attempt = meta
            .downField("attempt")
            .focus
            .flatMap(_.asNumber)
            .flatMap(_.toInt)
            .getOrElse(1) + 1
          instance.slots = meta
            .downField("slots")
            .focus
            .flatMap(_.asNumber)
            .flatMap(_.toInt)
            .getOrElse(1)
          if (instance.slots < JobManager.MaxSlots) instance.slots += 1
        })
    }
  }

  def init(): Unit = if (!initialized) synchronized {
    val failedJobsFile = s"$LoggingDir/$FailedJobsFile"
    if (new File(failedJobsFile).exists) {
      failed = ListMap(
        IOUtil
          .lines(failedJobsFile)
          .map(_.trim)
          .filter(_.nonEmpty)
          .map { line =>
            (strToKey(line), line)
          }
          .toSeq: _*)
    }

    val runningJobsFile = s"$LoggingDir/$RunningJobsFile"
    if (new File(runningJobsFile).exists) {
      val source = Source.fromFile(runningJobsFile, Charset)
      try {
        running = ListMap(parse(source.mkString).right.toOption.toSeq.flatMap { json =>
          val cursor = json.hcursor
          cursor.keys.toIterator.flatten.map { key =>
            key -> ListMap({
              val map = cursor.downField(key)
              map.keys.toSeq.flatten.flatMap(k => map.downField(k).focus.map(k -> _))
            }: _*)
          }
        }: _*)
      } finally {
        source.close()
      }
    }

    initialized = true

    if (!Arch.debugging) {
      val resuming = running
      running = ListMap.empty
      for {
        (_, values) <- resuming
        id <- values.get("id").flatMap(_.asString)
        conf <- values.get("conf").flatMap(DerivationJobConf.fromJson)
      } {
        for (job <- JobManager.get(id)) {
          for (uuid <- values.get("uuid").flatMap(_.asString)) {
            Try(Keystone.registerJobEvent(uuid, "CANCELLED"))
          }
          job.reset(conf)
          job.enqueue(
            conf,
            { instance =>
              instance.user = values
                .get("user")
                .flatMap(_.asString)
                .filter(_.nonEmpty)
                .flatMap(ArchUser.get(_))
              instance.attempt = values
                .get("attempt")
                .flatMap(_.asNumber)
                .flatMap(_.toInt)
                .getOrElse(1)
              instance.slots = values
                .get("slots")
                .flatMap(_.asNumber)
                .flatMap(_.toInt)
                .getOrElse(1)
            })
        }
      }
    }
  }

  def key(instance: DerivationJobInstance): String = instance.job.id + instance.conf.serialize

  def str(instance: DerivationJobInstance): String = {
    instance.job.id + " (" + instance.hashCode.abs + ") " + instance.conf.serialize + s" $MetaSeparator " + {
      ListMap(metaInfo(instance): _*).asJson.noSpaces
    }
  }

  def strToKey(str: String): String = {
    val indexOfMeta = str.lastIndexOf(MetaSeparator)
    val split = (if (indexOfMeta < 0) str else str.take(indexOfMeta)).split(" ", 3)
    split(0).trim + split(2).trim
  }

  def println(str: String): Unit = {
    val msg = "[" + Instant.now.toString + "] " + str
    Console.println(msg)
    synchronized {
      val print =
        new PrintStream(new FileOutputStream(s"$LoggingDir/$JobLogFile", true), true, Charset)
      try print.println(msg)
      finally print.close()
    }
  }

  def logRegister(instance: DerivationJobInstance): Unit = {
    println("Registered: " + str(instance))
    Try(Keystone.registerJobStart(instance))
  }

  def logUnregister(instance: DerivationJobInstance): Unit = {
    println("Unregistered: " + str(instance))
    Try(Keystone.registerJobComplete(instance))
  }

  def logQueued(instance: DerivationJobInstance, subJob: Boolean = false): Unit = {
    if (!subJob) {
      registerRunning(instance)
      Try(Keystone.registerJobEvent(instance.uuid, "QUEUED"))
    }
    println("Queued: " + str(instance))
  }

  def logRunning(instance: DerivationJobInstance, subJob: Boolean = false): Unit = {
    if (!subJob) {
      registerRunning(instance)
      Try(Keystone.registerJobEvent(instance.uuid, "RUNNING"))
    }
    println("Running: " + str(instance))
  }

  def logFinished(instance: DerivationJobInstance, subJob: Boolean = false): Unit = {
    if (!subJob) {
      unregisterRunning(instance)
      Try(Keystone.registerJobEvent(instance.uuid, "FINISHED"))
      if (InputSpec.isCollectionBased(instance.conf.inputSpec)) {
        for {
          u <- instance.user
          email <- u.email
        } {
          for (template <- instance.job.finishedNotificationTemplate) {
            val collection = instance.conf.inputSpec.collection
            MailUtil.sendTemplate(
              template,
              Map(
                "to" -> email,
                "collectionsUrl" -> ViewPathPatterns.reverseAbs(ViewPathPatterns.Collections),
                "datasetUrl" -> ViewPathPatterns.reverseAbs(
                  ViewPathPatterns.Dataset,
                  Map(
                    "dataset_id" -> DatasetUtil.formatId(collection.userUrlId(u.id), instance.job),
                    "sample" -> instance.conf.isSample.toString)),
                "jobName" -> instance.job.name,
                "collectionName" -> collection.name,
                "userName" -> u.fullName,
                "udqCollectionName" -> instance.conf.params.get[String]("name").getOrElse("")))
          }
        }
      }
    }
    println("Finished: " + str(instance))
  }

  def logFailed(instance: DerivationJobInstance, subJob: Boolean = false): Unit = {
    if (!ShutdownHookManager.get().isShutdownInProgress) {
      if (!subJob) {
        registerFailed(instance)
        Try(Keystone.registerJobEvent(instance.uuid, "FAILED"))
        if (InputSpec.isCollectionBased(instance.conf.inputSpec) && {
          !Arch.debugging && instance.attempt >= JobManager.MaxAttempts
        }) {
            for (template <- instance.job.failedNotificationTemplate) {
              val collection = instance.conf.inputSpec.collection
              MailUtil.sendTemplate(
                template,
                Map(
                  "jobName" -> instance.job.name,
                  "jobId" -> instance.job.id,
                  "collectionName" -> collection.name,
                  "accountId" -> instance.user.map(_.id).getOrElse("N/A"),
                  "userName" -> instance.user.map(_.fullName).getOrElse("anonymous")))
            }
        }
      }
      println("Failed: " + str(instance))
    }
  }
}

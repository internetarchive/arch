package org.archive.webservices.ars.processing

import java.io.{File, FileOutputStream, PrintStream}
import java.time.Instant

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import io.circe.Json
import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.util.MailUtil
import org.archive.webservices.sparkling.io.IOUtil

import scala.collection.immutable.ListMap
import scala.io.Source
import scala.util.Try

object JobStateManager {
  val LoggingDir = "logging"
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
    val r = action
    r
  }

  private def saveRunning(): Unit = access {
    IOUtil.writeLines(s"$LoggingDir/$RunningJobsFile", Iterator(running.asJson.spaces4))
  }

  private def registerRunning(instance: DerivationJobInstance): Unit = access {
    unregisterFailed(instance)
    running = running.updated(
      key(instance),
      ListMap(
        "id" -> instance.job.id.asJson,
        "conf" -> instance.conf.toJson,
        "state" -> instance.stateStr.asJson,
        "user" -> instance.user.map(_.id).getOrElse("").asJson) ++ {
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

  def init(): Unit = if (!initialized) synchronized {
    val failedJobsFile = s"$LoggingDir/$FailedJobsFile"
    if (new File(failedJobsFile).exists) {
      failed = ListMap(
        IOUtil
          .lines(s"$LoggingDir/$FailedJobsFile")
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
        running =
          Try(parse(source.mkString).right.asInstanceOf[ListMap[String, Map[String, Json]]])
            .getOrElse(ListMap.empty)
      } finally {
        source.close()
      }
    }

    initialized = true

    if (!Arch.debugging) {
      for {
        (key, values) <- running
        id <- values.get("id").flatMap(_.asString)
        conf <- values.get("conf").flatMap(DerivationJobConf.fromJson)
      } {
        for (job <- JobManager.get(id)) {
          job.reset(conf)
          job.enqueue(
            conf, { instance =>
              instance.user = values
                .get("user")
                .flatMap(_.asString)
                .filter(_.nonEmpty)
                .flatMap(ArchUser.getInternal(_))
              instance.collection = ArchCollection.getInternal(conf.collectionId)
            })
        }
      }
      running = ListMap.empty
      saveRunning()
    }
  }

  def key(instance: DerivationJobInstance): String = instance.job.id + instance.conf.serialize

  def str(instance: DerivationJobInstance): String = {
    instance.job.id + " (" + instance.hashCode.abs + ") " + instance.conf.serialize + s" $MetaSeparator " + instance.user
      .map("user" -> _.id)
      .toMap
      .asJson
      .noSpaces
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
  }

  def logUnregister(instance: DerivationJobInstance): Unit = {
    println("Unregistered: " + str(instance))
  }

  def logQueued(instance: DerivationJobInstance, subJob: Boolean = false): Unit = {
    if (!subJob) registerRunning(instance)
    println("Queued: " + str(instance))
  }

  def logRunning(instance: DerivationJobInstance, subJob: Boolean = false): Unit = {
    if (!subJob) registerRunning(instance)
    println("Running: " + str(instance))
  }

  def logFinished(instance: DerivationJobInstance, subJob: Boolean = false): Unit = {
    if (!subJob) {
      unregisterRunning(instance)
      for {
        u <- instance.user
        email <- u.email
      } {
        MailUtil.sendTemplate(
          "finished",
          Map(
            "to" -> email,
            "jobName" -> instance.job.name,
            "jobId" -> instance.job.id,
            "collectionId" -> instance.conf.collectionId,
            "collectionName" -> instance.collection
              .map(_.name)
              .getOrElse(instance.conf.collectionId),
            "accountId" -> u.id,
            "userName" -> u.fullName))
      }
    }
    println("Finished: " + str(instance))
  }

  def logFailed(instance: DerivationJobInstance, subJob: Boolean = false): Unit = {
    if (!subJob) {
      registerFailed(instance)
      MailUtil.sendTemplate(
        "failed",
        Map(
          "jobName" -> instance.job.name,
          "jobId" -> instance.job.id,
          "collectionName" -> instance.collection
            .map(_.name)
            .getOrElse(instance.conf.collectionId),
          "accountId" -> instance.user.map(_.id).getOrElse("N/A"),
          "userName" -> instance.user.map(_.fullName).getOrElse("anonymous")))
    }
    println("Failed: " + str(instance))
  }
}
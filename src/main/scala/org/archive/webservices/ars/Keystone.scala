package org.archive.webservices.ars

import _root_.io.circe.syntax._
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.api.DatasetFile
import org.archive.webservices.ars.processing.{DerivationJobInstance, JobManager}

import java.io.DataOutputStream
import java.net.{HttpURLConnection, URL}
import java.time.Instant
import scala.io.Source
import scala.util.{Failure, Success, Try}

object Keystone {
  val jobStartEndpoint: String = "/private/api/job/start"
  val jobEventEndpoint: String = "/private/api/job/event"
  val jobCompleteEndpoint: String = "/private/api/job/complete"

  val maxRetries: Int = 3

  def registerJobStart(instance: DerivationJobInstance): Unit = {
    val jobName = instance.job.id
    if (!JobManager.jobs.contains(jobName)) {
      return
    }
    val inputMetadata = Map(
      "id" -> instance.uuid.asJson,
      "job_type_id" -> instance.job.uuid.asJson,
      "username" -> instance.user.map(_.userName).getOrElse("").toString.asJson,
      "input_bytes" -> instance.inputSize.asJson,
      "sample" -> instance.conf.isSample.asJson,
      "parameters" -> Map(
        "instance_hashcode" -> instance.hashCode.abs.toString.asJson,
        "attempt" -> instance.attempt.asJson,
        "conf" -> instance.conf.toJson).asJson,
      "commit_hash" -> ArchConf.version.getOrElse("").asJson,
      "created_at" -> Instant.now.toString.asJson).asJson.noSpaces
    val result = retryHttpRequest(jobStartEndpoint, inputMetadata, maxRetries)
    printHttpRequestOutput(result)
  }

  def registerJobEvent(uuid: String, currentState: String): Unit = {
    val eventMetadata = Map(
      "job_start_id" -> uuid,
      "event_type" -> currentState,
      "created_at" -> Instant.now.toString).asJson.noSpaces

    val result = retryHttpRequest(jobEventEndpoint, eventMetadata, maxRetries)
    printHttpRequestOutput(result)
  }

  def registerJobComplete(instance: DerivationJobInstance): Unit = {
    if (!JobManager.jobs.contains(instance.job.id)) {
      return
    }

    val outputMetadata = Map(
      "job_start_id" -> instance.uuid.asJson,
      "output_bytes" -> instance.outputSize.asJson,
      "created_at" -> Instant.now.toString.asJson,
      "files" -> (
        // Temporarily skip retrieving files for WAT/WANE and ArchiveSpark* job types
        // until peformance issue is resolved, see: WT-2870
        if (
          instance.job == org.archive.webservices.ars.processing.jobs.ArsWatGeneration
            || instance.job == org.archive.webservices.ars.processing.jobs.ArsWaneGeneration
            || instance.job == org.archive.webservices.ars.processing.jobs.archivespark.ArchiveSparkEntityExtraction
            || instance.job == org.archive.webservices.ars.processing.jobs.archivespark.ArchiveSparkEntityExtractionChinese
        )
          Seq.empty.asInstanceOf[Seq[_root_.io.circe.Json]].asJson
        else
          instance.outFiles
          .map(DatasetFile.apply)
          .map(_.toJson)
          .toSeq
          .asJson
      )
    ).asJson.noSpaces


    val result = retryHttpRequest(jobCompleteEndpoint, outputMetadata, maxRetries)
    printHttpRequestOutput(result)
  }

  def keystoneHttpRequest(endpoint: String, data: String): Try[String] = Try {
    val keystoneBaseUrl = ArchConf.keystoneBaseUrl.getOrElse("")
    val connection =
      new URL(keystoneBaseUrl + endpoint).openConnection.asInstanceOf[HttpURLConnection]

    try {
      connection.setRequestMethod("POST")
      connection.setDoOutput(true)
      connection.setRequestProperty("Accept", "application/json")
      connection.setRequestProperty("X-API-KEY", ArchConf.keystonePrivateApiKey.getOrElse(""))

      val outputStream = new DataOutputStream(connection.getOutputStream)
      try {
        outputStream.writeBytes(data)
        outputStream.flush()
      } finally {
        Try(outputStream.close())
      }

      val responseCode = connection.getResponseCode
      if (responseCode == HttpURLConnection.HTTP_OK
        || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
        val inputStream = connection.getInputStream
        val response = Source.fromInputStream(inputStream).mkString

        inputStream.close()
        response
      } else {
        throw new RuntimeException("HTTP request failed with response code: " + responseCode)
      }
    } finally {
      Try(connection.disconnect())
    }
  }

  def retryHttpRequest(url: String, postData: String, triesRemaining: Int): Try[String] = {
    keystoneHttpRequest(url, postData) match {
      case Success(response) => Success(response)
      case Failure(_) if triesRemaining > 0 =>
        println("Retrying HTTP request, attempt: " + (maxRetries - triesRemaining + 1))
        retryHttpRequest(url, postData, triesRemaining - 1)
      case Failure(exception) => {
        // report failure to sentry
        Try(
          Arch.reportError(
            s"Keystone Request Error - $url",
            exception.getMessage,
            Map("url" -> url, "method" -> "POST", "data" -> postData)))
        Failure(exception)
      }
    }
  }

  def printHttpRequestOutput(result: Try[String]): Unit = {
    result match {
      case Success(response) => println("Success: " + response)
      case Failure(exception) => println("Failure: " + exception.getMessage)
    }
  }
}

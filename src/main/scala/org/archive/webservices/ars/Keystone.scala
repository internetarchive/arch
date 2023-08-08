package org.archive.webservices.ars

import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import _root_.io.circe.Json
import org.archive.webservices.ars.processing.DerivationJobInstance
import org.archive.webservices.ars.processing.JobManager
import org.archive.webservices.ars.model.ArchConf
import java.time.Instant
import scala.util.{Try, Success, Failure}

import java.io.{DataOutputStream}
import java.net.{HttpURLConnection, URL}
import scala.io.Source

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

    // TODO: remove extra backslashes from params output
    val params = Map(
      "instance_hashcode" -> instance.hashCode.abs.toString,
      "attempt" -> instance.attempt.toString,
      "conf" -> instance.conf.serialize).asJson.noSpaces

    val inputMetadata = Map(
      "id" -> instance.uuid,
      "job_type_id" -> instance.job.uuid,
      "username" -> instance.user.map(_.userName).getOrElse("").toString,
      "input_bytes" -> instance.inputSize.toString,
      "sample" -> instance.conf.isSample.toString,
      "created_at" -> Instant.now.toString,
      "parameters" -> params,
      "commit_hash" -> ArchConf.version.getOrElse("")).asJson.noSpaces

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

    val outputSize = instance.outFiles.map(_.size).sum
    val outputMetadata = Map(
      "job_start_id" -> instance.uuid,
      "output_bytes" -> outputSize.toString,
      "created_at" -> Instant.now.toString).asJson.noSpaces

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
      if (responseCode == HttpURLConnection.HTTP_OK) {
        val inputStream = connection.getInputStream
        val response = Source.fromInputStream(inputStream).mkString

        inputStream.close()
        response
      } else {
        // report error to sentry
        val source = Source.fromInputStream(connection.getErrorStream, "utf-8")
        try
          Arch.reportError(
            "Keystone Http Request Error",
            source.mkString,
            Map(
              "status_code" -> responseCode.toString,
              "path" -> endpoint,
              "method" -> connection.getRequestMethod))
        finally source.close()

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
      case Failure(exception) => Failure(exception)
    }
  }

  def printHttpRequestOutput(result: Try[String]): Unit = {
    result match {
      case Success(response) => println("Success: " + response)
      case Failure(exception) => println("Failure: " + exception.getMessage)
    }
  }
}

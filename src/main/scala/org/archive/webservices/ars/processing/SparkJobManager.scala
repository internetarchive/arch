package org.archive.webservices.ars.processing

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.archive.helge.sparkling.util.SparkUtil
import org.archive.webservices.ars.model.ArsCloudConf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object SparkJobManager {
  val SharedSparkContext = true
  val MaxRunning = 5

  private val queue = collection.mutable.Queue.empty[DerivationJobInstance]
  private var running = 0

  lazy val context: Future[SparkContext] = {
    val context = SparkUtil.config(
      SparkSession.builder,
      appName = "ARS-cloud",
      executors = 5,
      executorCores = 4,
      executorMemory = "2g",
      additionalConfigs = Map("spark.master" -> ArsCloudConf.sparkMaster),
      verbose = true)
    context.setLogLevel("INFO")
    println("Spark context initialized: " + context.applicationId)
    Future(context)
  }

  def initialized: Boolean = context.isCompleted && context.value.get.isSuccess

  def init(): Unit = if (SharedSparkContext) Await.ready(context, Duration.Inf)

  def enqueue(instance: DerivationJobInstance): Option[DerivationJobInstance] =
    queue.synchronized {
      if (JobManager.register(instance)) {
        queue.enqueue(instance)
        processQueue()
        Some(instance)
      } else None
    }

  def run(job: DerivationJob, conf: DerivationJobConf): Future[Boolean] = {
    if (SharedSparkContext) job.run(conf) else SparkRunner.run(job, conf)
  }

  private def processQueue(): Unit = queue.synchronized {
    if (running < MaxRunning && queue.nonEmpty) {
      val instance = queue.dequeue
      instance.updateState(ProcessingState.Running)
      running += 1
      run(instance.job, instance.conf).onComplete { opt =>
        JobManager.unregister(instance)
        val success = opt.toOption.getOrElse(false)
        instance.updateState(if (success) ProcessingState.Finished else ProcessingState.Failed)
        running -= 1
        processQueue()
      }
    }
  }
}

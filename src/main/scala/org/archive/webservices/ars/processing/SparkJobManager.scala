package org.archive.webservices.ars.processing

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.archive.helge.sparkling.Sparkling.executionContext
import org.archive.helge.sparkling.util.SparkUtil
import org.archive.webservices.ars.model.ArsCloudConf

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object SparkJobManager extends JobManagerBase("Spark", 3) {
  val SharedSparkContext = true

  lazy val context: Future[SparkContext] = {
    val context = SparkUtil.config(
      SparkSession.builder,
      appName = "ARS-cloud",
      executors = 5,
      executorCores = 3,
      executorMemory = "12g",
      queue = ArsCloudConf.hadoopQueue,
      additionalConfigs = Map("spark.master" -> ArsCloudConf.sparkMaster),
      verbose = true)
    context.setLogLevel("INFO")
    println("Spark context initialized: " + context.applicationId)
    Future(context)
  }

  def initialized: Boolean = context.isCompleted && context.value.get.isSuccess

  def init(): Unit = if (SharedSparkContext) Await.ready(context, Duration.Inf)

  def run(job: DerivationJob, conf: DerivationJobConf): Future[Boolean] = {
    if (SharedSparkContext) job.run(conf) else SparkRunner.run(job, conf)
  }
}

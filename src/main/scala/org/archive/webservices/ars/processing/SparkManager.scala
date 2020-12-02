package org.archive.webservices.ars.processing

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.archive.helge.sparkling.util.SparkUtil
import org.archive.webservices.ars.model.ArsCloudConf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SparkManager {
  val MaxRunning = 5

  private val queue = collection.mutable.Queue.empty[DerivationJobInstance]
  private var running = 0

  val context: Future[SparkContext] = {
    val context = SparkUtil.config(SparkSession.builder, appName = "ARS-cloud", executors = 5, executorCores = 4, executorMemory = "2g", additionalConfigs = Map(
      "spark.master" -> ArsCloudConf.sparkMaster
    ))
    Future(context)
  }

  def initialized: Boolean = context.isCompleted && context.value.get.isSuccess

  def init(): Unit = for (c <- context) println("Spark context initialized: " + c.startTime)

  def enqueue(instance: DerivationJobInstance): Boolean = queue.synchronized {
    if (JobManager.register(instance)) {
      queue.enqueue(instance)
      processQueue()
      true
    } else false
  }

  private def processQueue(): Unit = queue.synchronized {
    if (running < MaxRunning && queue.nonEmpty) {
      val instance = queue.dequeue
      instance.state = ProcessingState.Running
      running += 1
      instance.job.run(instance.conf).onComplete { opt =>
        val success = opt.toOption.getOrElse(false)
        instance.state = if (success) ProcessingState.Finished else ProcessingState.Failed
        JobManager.unregister(instance)
        running += 1
        processQueue()
      }
    }
  }
}

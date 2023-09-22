package org.archive.webservices.ars.processing

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.archive.webservices.sparkling._
import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.util.SparkUtil

import java.io.File
import scala.concurrent.Future

object SparkJobManager extends JobManagerBase("Spark", 3, timeoutSecondsMinMax = Some((60 * 60, 60 * 60 * 3))) {
  val SharedSparkContext = true
  val SparkAllocationFile = "fairscheduler.xml"
  val MaxPriorityWeight = 128
  val PoolPrefix = "weight-"

  private var _context: Option[SparkContext] = None

  def context: Future[SparkContext] = {
    Future {
      synchronized(_context.getOrElse {
        val context = SparkUtil.config(
          SparkSession.builder,
          appName = "ARCH",
          executors = 15,
          executorCores = 4,
          executorMemory = "16g",
          queue = ArchConf.hadoopQueue,
          additionalConfigs = Map(
            "spark.master" -> ArchConf.sparkMaster,
            "spark.scheduler.mode" -> "FAIR",
            "spark.yarn.executor.memoryOverhead" -> (4.gb / 1.mb).toString, // off-heap memory in MiB
            "spark.scheduler.allocation.file" -> new File(SparkAllocationFile).getAbsolutePath),
          verbose = true)
        context.setLogLevel("INFO")
        _context = Some(context)
        Sparkling.resetSparkContext(Some(context))
        println("New Spark context initialized: " + context.applicationId)
        context
      })
    }
  }

  def initThread(sc: SparkContext, job: DerivationJob, conf: DerivationJobConf): Unit = {
    sc.setJobGroup(job.id + "-" + conf.hashCode, job.name + " " + conf.serialize)
    sc.setLocalProperty("spark.scheduler.pool", PoolPrefix + currentPriority)
  }

  def stopContext(): Unit = synchronized {
    for (context <- _context) {
      context.stop()
      while (!context.isStopped) Thread.`yield`()
      _context = None
      Sparkling.resetSparkContext()
    }
  }

  override protected def onAllJobsFinished(): Unit = synchronized {
    super.onAllJobsFinished()
    if (!Arch.debugging) stopContext()
  }

  override protected def onPriorityJobsFinished(priority: Int): Unit = synchronized {
    super.onPriorityJobsFinished(priority)
    removePriority(priority)
  }

  override protected def onTimeout(instances: Seq[DerivationJobInstance]): Unit = synchronized {
    super.onTimeout(instances)
//    stopContext()
//    for (instance <- instances) instance.job.reset(instance.conf)
    bypassJobs()
  }

  def bypassJobs(): Boolean = synchronized {
    if (currentPriority < MaxPriorityWeight && priorityRunningCount > 0) {
      newPriority(currentPriority * 2)
      true
    } else false
  }

  def run(job: DerivationJob, conf: DerivationJobConf): Future[Boolean] = {
    if (SharedSparkContext) job.run(conf) else SparkRunner.run(job, conf)
  }
}

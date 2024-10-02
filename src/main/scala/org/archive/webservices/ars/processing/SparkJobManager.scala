package org.archive.webservices.ars.processing

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.util.SparkUtil
import org.archive.webservices.sparkling.{Sparkling, _}

import java.io.File
import scala.concurrent.Future
import java.time.Instant

object SparkJobManager
    extends JobManagerBase("Spark", 3, timeoutSecondsMinMax = Some((60 * 60, 60 * 60 * 3))) {
  val SharedSparkContext = true
  val SparkAllocationFile = "fairscheduler.xml"
  val MaxPriorityWeight = 128
  val PoolPrefix = "weight-"

  private var _context: Option[SparkContext] = None

  def context: Future[SparkContext] = {
    Future {
      synchronized(_context.filter(!_.isStopped).getOrElse {
        val context = SparkUtil.config(
          SparkSession.builder,
          appName = s"ARCH ${ArchConf.deploymentEnvironment}",
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
        context.addSparkListener(SparkJobListener)
        println("New Spark context initialized: " + context.applicationId)
        context
      })
    }
  }

  private def priorityWeight: Int = if (currentPriority == 0) 1 else currentPriority

  def initThread(sc: SparkContext, job: DerivationJob, conf: DerivationJobConf): Unit = {
    sc.setJobGroup(job.uuid, job.name + " " + conf.serialize)
    sc.setLocalProperty("spark.scheduler.pool", PoolPrefix + priorityWeight)
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

  override protected def onTimeout(instances: Seq[DerivationJobInstance]): Unit = synchronized {
    super.onTimeout(instances)
    if (timeoutSecondsMinMax.isDefined) {
      val (timeoutSecondsMin, timeoutSecondsMax) = timeoutSecondsMinMax.get
      val minThreshold = Instant.now.getEpochSecond - timeoutSecondsMin
      val maxThreshold = Instant.now.getEpochSecond - timeoutSecondsMax
      val startTimes = SparkJobListener.taskStartTimes.values
      if (startTimes.forall(_ < minThreshold) && startTimes.exists(_ < maxThreshold)) {
        SparkJobListener.synchronized {
          SparkJobListener.reset()
          stopContext()
          return
        }
      }
    }
    if (numQueued > 0 && freeSlots == 0) bypassJobs()
  }

  def bypassJobs(): Boolean = synchronized {
    if (priorityWeight < MaxPriorityWeight && priorityRunningCount > 0) {
      newPriority(priorityWeight * 2)
      true
    } else false
  }

  def run(job: DerivationJob, conf: DerivationJobConf): Future[Boolean] = {
    if (SharedSparkContext) job.run(conf) else SparkRunner.run(job, conf)
  }
}

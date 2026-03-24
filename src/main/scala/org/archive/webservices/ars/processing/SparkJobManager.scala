package org.archive.webservices.ars.processing

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.util.SparkUtil
import org.archive.webservices.sparkling.{Sparkling, _}

import java.io.File
import java.time.Instant
import scala.concurrent.Future

object SparkJobManager
    extends JobManagerBase("Spark", 3, timeoutSecondsMinMax = Some((60 * 60, 60 * 60 * 3))) {
  val taskTimeoutSeconds = 60 * 60 * 12 // 12 hours
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
            "spark.scheduler.allocation.file" -> new File(SparkAllocationFile).getAbsolutePath,
            "spark.yarn.am.memory" -> "4096m"),
          verbose = true)
        context.setLogLevel("INFO")
        _context = Some(context)
        Sparkling.resetSparkContext(Some(context))
        SparkJobListener.reset()
        context.addSparkListener(SparkJobListener)
        println("New Spark context initialized: " + context.applicationId)
        context
      })
    }
  }

  private def priorityWeight: Int = if (currentPriority == 0) 1 else currentPriority

  def initThread(sc: SparkContext, instance: DerivationJobInstance): Unit = {
    val (job, conf) = (instance.job, instance.conf)
    sc.setJobGroup(instance.uuid, job.name + " " + conf.serialize, interruptOnCancel = true)
    sc.setLocalProperty("spark.scheduler.pool", PoolPrefix + priorityWeight)
    // If the job was cancelled while the Spark context was still initializing,
    // cancelJobGroup in stopJob was a no-op (context was None). Throw here so
    // the run Future fails before any Spark actions are submitted; onComplete
    // will see state == Cancelled and handle it correctly.
    if (instance.state == ProcessingState.Cancelled)
      throw new RuntimeException(s"Job ${instance.uuid} was cancelled before execution started")
  }

  def stopJob(instance: DerivationJobInstance): Boolean = {
    for (context <- _context) context.cancelJobGroup(instance.uuid)
    true
  }

  def stopContext(): Unit = synchronized {
    for (context <- _context) {
      println("Stopping Spark context: " + context.applicationId)
      context.stop()
      while (!context.isStopped) Thread.`yield`()
      _context = None
      Sparkling.resetSparkContext()
      println("Spark context stopped.")
    }
  }

  override protected def onAllJobsFinished(): Unit = synchronized {
    super.onAllJobsFinished()
    if (!Arch.debugging) stopContext()
  }

  override protected def onTimeout(instances: Seq[DerivationJobInstance]): Unit = synchronized {
    val threshold = Instant.now.getEpochSecond - taskTimeoutSeconds
    val startTimes = SparkJobListener.taskStartTimes.values
    if (startTimes.forall(_ < threshold)) {
      stopContext()
      return
    }
    if (numQueued > 0 && freeSlots == 0) bypassJobs()
  }

  def bypassJobs(): Boolean = synchronized {
    if (priorityWeight < MaxPriorityWeight && priorityRunningCount > 0) {
      newPriority(priorityWeight * 2)
      true
    } else false
  }
}

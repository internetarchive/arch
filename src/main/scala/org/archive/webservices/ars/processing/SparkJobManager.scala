package org.archive.webservices.ars.processing

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.archive.webservices.ars.Arch
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.util.SparkUtil

import scala.concurrent.Future

object SparkJobManager extends JobManagerBase("Spark", 3, timeoutSeconds = 60 * 60 * 24) {
  val SharedSparkContext = true

  private var _context: Option[SparkContext] = None

  def context: Future[SparkContext] = Future {
    synchronized(_context.getOrElse {
      val context = SparkUtil.config(
        SparkSession.builder,
        appName = "ARCH",
        executors = 10,
        executorCores = 4,
        executorMemory = "16g",
        queue = ArchConf.hadoopQueue,
        additionalConfigs =
          Map("spark.master" -> ArchConf.sparkMaster, "spark.scheduler.mode" -> "FAIR"),
        verbose = true)
      context.setLogLevel("INFO")
      _context = Some(context)
      Sparkling.resetSparkContext(Some(context))
      println("New Spark context initialized: " + context.applicationId)
      context
    })
  }

  def stopContext(): Unit = synchronized {
    if (!Arch.debugging) {
      for (context <- _context) {
        context.stop()
        while (!context.isStopped) Thread.`yield`()
        _context = None
        Sparkling.resetSparkContext()
      }
    }
  }

  override protected def onAllJobsFinished(): Unit = {
    super.onAllJobsFinished()
    stopContext()
  }

  override protected def onTimeout(instances: Seq[DerivationJobInstance]): Unit = {
    super.onTimeout(instances)
    stopContext()
    for (instance <- instances) instance.job.reset(instance.conf)
  }

  def run(job: DerivationJob, conf: DerivationJobConf): Future[Boolean] = {
    if (SharedSparkContext) job.run(conf) else SparkRunner.run(job, conf)
  }
}

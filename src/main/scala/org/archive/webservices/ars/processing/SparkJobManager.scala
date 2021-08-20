package org.archive.webservices.ars.processing

import java.io.File

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.archive.helge.sparkling.Sparkling.executionContext
import org.archive.helge.sparkling.util.SparkUtil
import org.archive.webservices.ars.model.ArchConf

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
        additionalConfigs = Map("spark.master" -> ArchConf.sparkMaster),
        verbose = true)
      context.setLogLevel("INFO")
      _context = Some(context)
      println("New Spark context initialized: " + context.applicationId)
      context
    })
  }

  def stopContext(): Unit = synchronized {
    for (context <- _context) {
      if (!new File("_debugging").exists) {
        context.stop()
        while (!context.isStopped) Thread.`yield`()
      }
      _context = None
    }
  }

  override protected def onAllJobsFinished(): Unit = {
    super.onAllJobsFinished()
    stopContext()
  }

  override protected def onTimeout(instances: Seq[DerivationJobInstance]): Unit = {
    super.onTimeout(instances)
    stopContext()
  }

  def run(job: DerivationJob, conf: DerivationJobConf): Future[Boolean] = {
    if (SharedSparkContext) job.run(conf) else SparkRunner.run(job, conf)
  }
}

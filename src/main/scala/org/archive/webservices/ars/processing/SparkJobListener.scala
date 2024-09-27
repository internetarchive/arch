package org.archive.webservices.ars.processing

import org.apache.spark.scheduler.{SparkListener, SparkListenerTaskEnd, SparkListenerTaskStart, TaskInfo}

import java.time.Instant

object SparkJobListener extends SparkListener {
  private val _taskStartTimes = collection.mutable.Map.empty[String, Long]

  def taskStartTimes: Map[String, Long] = _taskStartTimes.toMap

  def id(info: TaskInfo): String = info.id + "#" + info.taskId

  override def onTaskStart(taskStart: SparkListenerTaskStart): Unit = synchronized {
    _taskStartTimes(id(taskStart.taskInfo)) = Instant.now.getEpochSecond
  }

  override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit = synchronized {
    _taskStartTimes.remove(id(taskEnd.taskInfo))
  }

  def reset(): Unit = synchronized(_taskStartTimes.clear())
}

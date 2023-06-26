package org.archive.webservices.ars.processing

import org.archive.webservices.sparkling.Sparkling.executionContext

import java.time.Instant
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import scala.concurrent.Future

class JobManagerBase(
    val name: String,
    val maxJobsRunning: Int = 5,
    val timeoutSeconds: Int = -1) {
  val TimeoutCheckPeriodSeconds: Int = 60 * 5

  private val mainQueue = new JobQueue(name + " Queue")
  private val sampleQueue = new JobQueue(name + " Example Queue")
  private val queues = Seq(mainQueue, sampleQueue)

  private var nextQueueIdx = 0
  private val running = collection.mutable.Queue.empty[(DerivationJobInstance, Long)]
  private var priorityRunning = collection.mutable.Queue.empty[DerivationJobInstance]
  private var _currentPriority: Int = 1

  private var priorities = Map(_currentPriority -> priorityRunning)

  private val timeoutExecutor = {
    new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(
      () => checkTimeout(),
      TimeoutCheckPeriodSeconds,
      TimeoutCheckPeriodSeconds,
      TimeUnit.SECONDS)
  }

  def currentPriority: Int = _currentPriority
  def runningCount: Int = running.size
  def priorityRunningCount: Int = priorityRunning.size

  def newPriority(priority: Int): Unit = synchronized {
    if (!priorities.contains(priority)) {
      priorityRunning = collection.mutable.Queue.empty[DerivationJobInstance]
      _currentPriority = priority
      priorities += currentPriority -> priorityRunning
      processQueues()
    }
  }

  def removePriority(priority: Int): Boolean = synchronized {
    if (priority > 1 && priorities.get(priority).exists(_.isEmpty)) {
      priorities -= priority
      val (p, running) = priorities.maxBy(_._1)
      _currentPriority = p
      priorityRunning = running
      true
    } else false
  }

  def enqueue(instance: DerivationJobInstance): Option[DerivationJobInstance] = {
    val queue = if (instance.conf.sample < 0) mainQueue else sampleQueue
    queue.synchronized {
      if (JobManager.register(instance)) {
        instance.setQueue(queue, queue.enqueue(instance))
        processQueues()
        Some(instance)
      } else None
    }
  }

  def checkTimeout(): Unit = synchronized {
    if (timeoutSeconds >= 0 && priorityRunning.size == maxJobsRunning && queues.exists(
          _.nonEmpty)) {
      val threshold = Instant.now.getEpochSecond - timeoutSeconds
      if (running.forall(_._2 < threshold)) onTimeout(running.map(_._1))
    }
  }

  protected def onTimeout(instances: Seq[DerivationJobInstance]): Unit = {}

  protected def onAllJobsFinished(): Unit = {}

  protected def onPriorityJobsFinished(priority: Int): Unit = {}

  private def nextQueue: Option[JobQueue] = {
    val next = (queues.drop(nextQueueIdx).toIterator ++ queues.take(nextQueueIdx).toIterator)
      .find(_.nonEmpty)
    nextQueueIdx = (nextQueueIdx + 1) % queues.size
    next
  }

  private def processQueues(): Unit = synchronized {
    while (priorityRunning.size < maxJobsRunning && queues.exists(_.nonEmpty)) {
      for (queue <- nextQueue) {
        queue.synchronized {
          val instance = queue.dequeue
          instance.unsetQueue()
          instance.updateState(ProcessingState.Running)
          running.enqueue((instance, Instant.now.getEpochSecond))
          priorityRunning.enqueue(instance)
          val currentPriorityRunning = priorityRunning
          val priority = currentPriority
          Future(instance.job).flatMap(_.run(instance.conf)).onComplete { opt =>
            synchronized {
              val success = opt.toOption.getOrElse(false)
              instance.updateState(
                if (success) ProcessingState.Finished else ProcessingState.Failed)
              currentPriorityRunning.dequeueFirst(_ == instance)
              if (currentPriorityRunning.isEmpty) onPriorityJobsFinished(priority)
              running.dequeueFirst(_._1 == instance)
              JobManager.unregister(instance)
              if (running.isEmpty) onAllJobsFinished()
              if (!success && opt.isFailure) opt.failed.get.printStackTrace()
            }
            processQueues()
          }
        }
      }
    }
    checkTimeout()
  }
}

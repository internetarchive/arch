package org.archive.webservices.ars.processing

import org.archive.webservices.sparkling.Sparkling.executionContext

import java.time.Instant
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import scala.concurrent.Future

class JobManagerBase(
    val name: String,
    val slots: Int = 5,
    val timeoutSecondsMinMax: Option[(Int, Int)] = None) {
  private val mainQueue = new JobQueue(name + " Queue")
  private val sampleQueue = new JobQueue(name + " Example Queue")
  private val queues = Seq(mainQueue, sampleQueue)

  private var nextQueueIdx = 0
  private var _currentPriority: Int = 0
  private var _isPriority = -1
  private val running = collection.mutable.Map.empty[DerivationJobInstance, Long]
  private var priorityRunning = collection.mutable.Queue.empty[DerivationJobInstance]
  private var priorities = Map(_currentPriority -> priorityRunning)
  private val recentUsers = collection.mutable.Queue.empty[String]
  private var depriotitizedSources = Set.empty[String]

  private val timeoutExecutor = {
    val timeoutCheckPeriodSeconds: Int = 60 * 5
    new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(
      () => checkTimeout(),
      timeoutCheckPeriodSeconds,
      timeoutCheckPeriodSeconds,
      TimeUnit.SECONDS)
  }

  def currentPriority: Int = _currentPriority
  def priorityRunningCount: Int = priorityRunning.size
  def isPriority: Boolean = _isPriority == _currentPriority
  def freeSlots: Int = slots - priorityRunning.map(_.slots).sum

  def newPriority(priority: Int): Unit = synchronized {
    if (!priorities.contains(priority)) {
      _currentPriority = priority
      _isPriority = priority
      priorityRunning = collection.mutable.Queue.empty[DerivationJobInstance]
      priorities += priority -> priorityRunning
      depriotitizedSources = running.map(_._1.collection.sourceId).toSet
      processQueues()
    }
  }

  def removePriority(priority: Int = _currentPriority): Boolean = synchronized {
    if (priority > 0 && priorities.get(priority).exists(_.isEmpty)) {
      priorities -= priority
      val (p, running) = priorities.maxBy(_._1)
      _currentPriority = p
      priorityRunning = running
      if (p == 0) depriotitizedSources = Set.empty
      true
    } else false
  }

  def enqueue(instance: DerivationJobInstance): Option[DerivationJobInstance] = synchronized {
    val queue = if (instance.conf.sample < 0) mainQueue else sampleQueue
    if (JobManager.register(instance)) {
      instance.setQueue(queue, queue.enqueue(instance))
      processQueues()
      Some(instance)
    } else None
  }

  def checkTimeout(): Unit = synchronized {
    if (timeoutSecondsMinMax.isDefined && queues.exists(_.nonEmpty) && freeSlots == 0) {
      val (timeoutSecondsMin, timeoutSecondsMax) = timeoutSecondsMinMax.get
      val minThreshold = Instant.now.getEpochSecond - timeoutSecondsMin
      val maxThreshold = Instant.now.getEpochSecond - timeoutSecondsMax
      val startTimes = priorityRunning.map(running)
      if (startTimes.forall(_ < minThreshold) && startTimes.exists(_ < maxThreshold))
        onTimeout(priorityRunning)
    }
  }

  protected def onTimeout(instances: Seq[DerivationJobInstance]): Unit = {}

  protected def onAllJobsFinished(): Unit = {}

  private def nextQueue: Option[JobQueue] = synchronized {
    val isPriority = this.isPriority
    val freeSlots = this.freeSlots
    val next = (queues.drop(nextQueueIdx).toIterator ++ queues.take(nextQueueIdx).toIterator)
      .find(_.items.exists(instance =>
        instance.slots <= freeSlots && (!isPriority || !depriotitizedSources.contains(
          instance.collection.sourceId))))
    if (next.isDefined) nextQueueIdx = (nextQueueIdx + 1) % queues.size
    next
  }

  private def processQueues(): Unit = synchronized {
    var nextQueue: Option[JobQueue] = None
    while ({
      nextQueue = this.nextQueue
      nextQueue.nonEmpty
    }) {
      for (queue <- nextQueue) {
        for (instance <- queue.dequeue(
            freeSlots,
            if (isPriority) depriotitizedSources else Set.empty,
            recentUsers)) {
          for (user <- instance.user) {
            recentUsers.dequeueFirst(_ == user.id)
            recentUsers.enqueue(user.id)
          }
          instance.unsetQueue()
          instance.updateState(ProcessingState.Running)
          priorityRunning.enqueue(instance)
          running(instance) = Instant.now.getEpochSecond
          val currentPriorityRunning = priorityRunning
          val priority = _currentPriority
          Future(instance.job).flatMap(_.run(instance.conf)).onComplete { opt =>
            synchronized {
              val success = opt.toOption.getOrElse(false)
              instance.updateState(
                if (success) ProcessingState.Finished else ProcessingState.Failed)
              currentPriorityRunning.dequeueFirst(_ == instance)
              if (currentPriorityRunning.isEmpty) removePriority(priority)
              running.remove(instance)
              JobManager.unregister(instance)
              if (running.isEmpty) onAllJobsFinished()
              if (!success && opt.isFailure) opt.failed.get.printStackTrace()
            }
            processQueues()
          }
        }
      }
    }
    if (priorityRunning.isEmpty) removePriority()
    checkTimeout()
  }
}

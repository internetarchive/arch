package org.archive.webservices.ars.processing

import java.time.Instant

import org.archive.webservices.ars.processing.SparkJobManager.run
import org.archive.helge.sparkling.Sparkling.executionContext

class JobManagerBase(name: String, maxJobsRunning: Int = 5, timeoutSeconds: Int = -1) {
  private val mainQueue = new JobQueue(name + " Queue")
  private val sampleQueue = new JobQueue(name + " Sample Queue")

  private val running = collection.mutable.Queue.empty[(DerivationJobInstance, Long)]

  private var nextQueueIdx = 0
  private val queues = Seq(mainQueue, sampleQueue)
  def nextQueue: Option[JobQueue] = queues.synchronized {
    val next = (queues.drop(nextQueueIdx).toIterator ++ queues.take(nextQueueIdx).toIterator)
      .find(_.nonEmpty)
    nextQueueIdx = (nextQueueIdx + 1) % queues.size
    next
  }

  def enqueue(instance: DerivationJobInstance): Option[DerivationJobInstance] = {
    val queue = if (instance.conf.sample < 0) mainQueue else sampleQueue
    queue.synchronized {
      if (JobManager.register(instance)) {
        instance.setQueue(queue, queue.enqueue(instance))
        processQueue(queue)
        Some(instance)
      } else None
    }
  }

  private def checkTimeout(queue: JobQueue): Unit = {
    if (timeoutSeconds >= 0) {
      queue.synchronized {
        val timeout = Instant.now.getEpochSecond - timeoutSeconds
        if (running.nonEmpty && running.forall(_._2 < timeout)) {
          onTimeout(running.map(_._1))
        }
      }
    }
  }

  protected def onTimeout(instances: Seq[DerivationJobInstance]): Unit = {}

  protected def onAllJobsFinished(): Unit = {}

  private def processQueue(queue: JobQueue): Unit = queues.synchronized {
    for (queue <- nextQueue) {
      queue.synchronized {
        if (running.size < maxJobsRunning && queue.nonEmpty) {
          val instance = queue.dequeue
          instance.unsetQueue()
          instance.updateState(ProcessingState.Running)
          running.enqueue((instance, Instant.now.getEpochSecond))
          run(instance.job, instance.conf).onComplete { opt =>
            JobManager.unregister(instance)
            val success = opt.toOption.getOrElse(false)
            instance.updateState(
              if (success) ProcessingState.Finished else ProcessingState.Failed)
            running.dequeueFirst(_._1 == instance)
            if (running.isEmpty) onAllJobsFinished()
            else checkTimeout(queue)
            if (!success && opt.isFailure) opt.failed.get.printStackTrace()
            processQueue(queue)
          }
        } else checkTimeout(queue)
      }
    }
  }
}

package org.archive.webservices.ars.processing

import org.archive.webservices.ars.processing.SparkJobManager.run
import org.archive.helge.sparkling.Sparkling.executionContext

class JobManagerBase(name: String, maxJobsRunning: Int = 5) {
  private val mainQueue = new JobQueue(name + " Queue")
  private val sampleQueue = new JobQueue(name + " Sample Queue")

  private var running = 0

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

  private def processQueue(queue: JobQueue): Unit = queues.synchronized {
    for (queue <- nextQueue) {
      queue.synchronized {
        if (running < maxJobsRunning && queue.nonEmpty) {
          val instance = queue.dequeue
          instance.unsetQueue()
          instance.updateState(ProcessingState.Running)
          running += 1
          run(instance.job, instance.conf).onComplete { opt =>
            JobManager.unregister(instance)
            val success = opt.toOption.getOrElse(false)
            instance.updateState(
              if (success) ProcessingState.Finished else ProcessingState.Failed)
            running -= 1
            if (!success && opt.isFailure) opt.failed.get.printStackTrace()
            processQueue(queue)
          }
        }
      }
    }
  }
}

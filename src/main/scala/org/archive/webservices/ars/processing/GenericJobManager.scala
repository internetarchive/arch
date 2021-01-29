package org.archive.webservices.ars.processing

import scala.concurrent.ExecutionContext.Implicits.global

object GenericJobManager {
  val MaxRunning = 5

  private val queue = collection.mutable.Queue.empty[DerivationJobInstance]
  private var running = 0

  def enqueue(instance: DerivationJobInstance): Option[DerivationJobInstance] = queue.synchronized {
    if (JobManager.register(instance)) {
      queue.enqueue(instance)
      processQueue()
      Some(instance)
    } else None
  }

  private def processQueue(): Unit = queue.synchronized {
    if (running < MaxRunning && queue.nonEmpty) {
      val instance = queue.dequeue
      instance.state = ProcessingState.Running
      running += 1
      instance.job.run(instance.conf).onComplete { opt =>
        JobManager.unregister(instance)
        val success = opt.toOption.getOrElse(false)
        instance.state = if (success) ProcessingState.Finished else ProcessingState.Failed
        running -= 1
        processQueue()
      }
    }
  }
}

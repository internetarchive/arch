package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.DerivativeOutput

import scala.concurrent.Future

abstract class ChainedJob extends DerivationJob {
  def children: Seq[PartialDerivationJob]

  def run(instance: DerivationJobInstance): Future[Boolean] =
    throw new UnsupportedOperationException("Cannot run a chained job: " + id)

  def currentChild(conf: DerivationJobConf): (DerivationJobInstance, Int) = {
    children.zipWithIndex.reverseIterator
      .map { case (job, idx) =>
        (job.history(conf), idx)
      }
      .find { case (child, idx) => child.state > ProcessingState.NotStarted || idx == 0 }.get
  }

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    val (child, idx) = currentChild(conf)
    val state = child.state
    instance.state = state match {
      case ProcessingState.Cancelled => state
      case ProcessingState.Failed => state
      case ProcessingState.Finished =>
        if (idx == children.size - 1) state else ProcessingState.Failed
      case _ => if (idx == 0) state else ProcessingState.Failed
    }
    instance
  }

  override def stop(instance: DerivationJobInstance): Boolean = {
    // Use the live active child instance rather than a history instance.
    // currentChild() creates history instances which read info.state from cache.
    // Since parent.updateState(Cancelled) has already been called before stop() is invoked,
    // the cache already has state=Cancelled, so a history instance would see Cancelled and
    // stopJob() would return early without ever calling cancelJobGroup on the Spark job.
    val active = instance.active
    if (active != instance) active.stopJob()
    // Return true: the parent job's state is already Cancelled at this point, regardless of
    // whether the child's underlying computation (e.g. a GenericJob) could be interrupted.
    true
  }

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] =
    children.flatMap(_.templateVariables(conf))

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] =
    children.toIterator.flatMap(_.outFiles(conf))

  private def onChildComplete(
      instance: DerivationJobInstance,
      idx: Int,
      success: Boolean): Unit = {
    instance.unsetStage()
    if (success) {
      if (idx + 1 < children.size) {
        val nextChild = children(idx + 1)
        val enqueued =
          nextChild.enqueue(
            instance.conf,
            child => {
              child.attempt = instance.attempt
              child.slots = instance.slots
              child.user = instance.user
              child.onStateChanged {
                if (child.state > ProcessingState.Running)
                  onChildComplete(instance, idx + 1, child.state == ProcessingState.Finished)
              }
              child.onUnregistered {
                if (instance.state > ProcessingState.Running) JobManager.unregister(instance)
              }
              instance.setStage(child)
            })
        if (enqueued.isEmpty) {
          instance.updateState(ProcessingState.Failed)
        }
      } else {
        instance.updateState(ProcessingState.Finished)
      }
    } else {
      instance.updateState(ProcessingState.Failed)
    }
  }

  override def enqueue(
      conf: DerivationJobConf,
      get: DerivationJobInstance => Unit = _ => {}): Option[DerivationJobInstance] = {
    if (children.nonEmpty) {
      super.enqueue(conf, get).filter { instance =>
        JobManager.register(instance) && {
          val enqueued =
            children.head.enqueue(
              conf,
              child => {
                child.attempt = instance.attempt
                child.slots = instance.slots
                child.user = instance.user
                child.onStateChanged {
                  if (child.state > ProcessingState.Running)
                    onChildComplete(instance, 0, child.state == ProcessingState.Finished)
                  else instance.updateState(child.state)
                }
                child.onUnregistered {
                  if (instance.state > ProcessingState.Running) JobManager.unregister(instance)
                }
                instance.setStage(child)
              })
          if (enqueued.isDefined) true
          else {
            JobManager.unregister(instance)
            false
          }
        }
      }
    } else None
  }

  override def isResumable(conf: DerivationJobConf): Boolean = false
}

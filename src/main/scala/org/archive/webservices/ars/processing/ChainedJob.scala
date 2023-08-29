package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.DerivativeOutput

import scala.concurrent.Future

abstract class ChainedJob extends DerivationJob {
  def children: Seq[PartialDerivationJob]

  def run(conf: DerivationJobConf): Future[Boolean] =
    throw new UnsupportedOperationException("Cannot run a chained job: " + id)

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    val currentChild = children.zipWithIndex.reverseIterator
      .map {
        case (job, idx) =>
          (job.history(conf), idx)
      }
      .find { case (child, idx) => child.state > ProcessingState.NotStarted || idx == 0 }
    for ((child, idx) <- currentChild) {
      val state = child.state
      instance.state = state match {
        case ProcessingState.Failed => state
        case ProcessingState.Finished =>
          if (idx == children.size - 1) state else ProcessingState.Failed
        case _ => if (idx == 0) state else ProcessingState.Failed
      }
    }
    instance
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
              child.slots = instance.slots
              child.user = instance.user
              child.collection = instance.collection
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
                child.slots = instance.slots
                child.user = instance.user
                child.collection = instance.collection
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
}

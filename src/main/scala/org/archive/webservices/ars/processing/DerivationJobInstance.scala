package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchCollection, ArchCollectionInfo, ArchJobInstanceInfo, DerivativeOutput}

import java.time.Instant

case class DerivationJobInstance(job: DerivationJob, conf: DerivationJobConf) {
  var registered = false
  var state: Int = ProcessingState.NotStarted

  var user: Option[ArchUser] = None
  var collection: Option[ArchCollection] = None

  private var _queue: Option[JobQueue] = None
  private var queuePos: Int = -1

  def unsetQueue(): Unit = {
    _queue = None
    queuePos = -1
  }

  def setQueue(queue: JobQueue, pos: Int): Unit = {
    _queue = Some(queue)
    queuePos = pos
    JobStateManager.updateRunning(this)
  }

  def queue: Option[JobQueue] = _queue

  def queueIndex: Int =
    _queue
      .map { q =>
        if (queuePos >= q.pos) queuePos - q.pos else queuePos + (Int.MaxValue - q.pos)
      }
      .getOrElse(-1)

  private var activeStage: Option[DerivationJobInstance] = None

  def setStage(instance: DerivationJobInstance): Unit = {
    activeStage = Some(instance)
    JobStateManager.updateRunning(this)
  }

  def unsetStage(): Unit = activeStage = None

  def active: DerivationJobInstance = activeStage.getOrElse(this)

  def info: ArchJobInstanceInfo = ArchJobInstanceInfo.get(conf.outputPath + "/" + job.id)

  def updateState(value: Int): Unit = {
    val prevState = state
    state = value
    for (func <- _onStateChanged) func()
    if (registered) {
      if (job.partialOf.isEmpty) {
        val now = Instant.now
        var info = this.info
        if (prevState == ProcessingState.NotStarted) {
          info = info.setStartTime(now)
        }
        state match {
          case ProcessingState.Queued =>
            JobStateManager.logQueued(this)
          case ProcessingState.Running =>
            JobStateManager.logRunning(this)
          case ProcessingState.Failed =>
            info = info.setFinishedTime(now)
            JobStateManager.logFailed(this)
          case ProcessingState.Finished =>
            info = info.setFinishedTime(now)
            ArchCollectionInfo
              .get(conf.collectionId)
              .setLastJob(job.id, conf.isSample, now)
              .save()
            JobStateManager.logFinished(this)
        }
        info.save(conf.outputPath + "/" + job.id)
      } else {
        state match {
          case ProcessingState.Queued =>
            JobStateManager.logQueued(this, subJob = true)
          case ProcessingState.Running =>
            JobStateManager.logRunning(this, subJob = true)
          case ProcessingState.Failed =>
            JobStateManager.logFailed(this, subJob = true)
          case ProcessingState.Finished =>
            JobStateManager.logFinished(this, subJob = true)
        }
      }
    }
  }

  def stateStr: String = ProcessingState.Strings(state)
  def templateVariables: Seq[(String, Any)] = job.templateVariables(conf)
  def outFiles: Iterator[DerivativeOutput] = job.outFiles(conf)

  private var _onStateChanged: Seq[() => Unit] = Seq.empty
  def onStateChanged(action: => Unit): Unit = _onStateChanged :+= (() => action)
}

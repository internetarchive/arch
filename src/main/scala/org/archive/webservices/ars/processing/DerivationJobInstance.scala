package org.archive.webservices.ars.processing

import org.archive.webservices.ars.io.UuidManager
import org.archive.webservices.ars.model._
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.sparkling.util.Common

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DerivationJobInstance(job: DerivationJob, conf: DerivationJobConf) {
  var predefUuid: Option[String] = None
  var inputRevision: Int = conf.inputSpec.revision

  lazy val uuid: String = {
    val uuid = predefUuid.orElse(info.uuid).getOrElse(UuidManager.uuid)
    info.uuid = Some(uuid)
    uuid
  }

  var registered = false
  var state: Int = ProcessingState.NotStarted

  var user: Option[ArchUser] = None

  def inputSize: Long = job.inputSize(conf)

  def outputSize: Long = job.outputSize(conf)

  var attempt: Int = 1
  var slots: Int = 1

  private var _queue: Option[JobQueue] = None

  def unsetQueue(): Unit = {
    _queue = None
  }

  def setQueue(queue: JobQueue): Unit = {
    _queue = Some(queue)
    JobStateManager.updateRunning(this)
  }

  def queue: Option[JobQueue] = _queue

  private var activeStage: Option[DerivationJobInstance] = None

  def setStage(instance: DerivationJobInstance): Unit = {
    activeStage = Some(instance)
    JobStateManager.updateRunning(this)
  }

  def unsetStage(): Unit = activeStage = None

  def active: DerivationJobInstance = activeStage.getOrElse(this)

  def outPath: String = conf.outputPath + job.relativeOutPath

  def info: ArchJobInstanceInfo = {
    if (job.generatesOutput) {
      val info = ArchJobInstanceInfo(outPath)
      info.conf = Some(conf)
      info
    } else ArchJobInstanceInfo.inMemory
  }

  def updateState(value: Int): Unit = {
    val prevState = state
    // Once a job has been cancelled, do not overwrite its state with anything else.
    // Callbacks and onComplete handlers may still fire after cancellation is set;
    // this guard ensures Cancelled is a terminal state.
    if (prevState == ProcessingState.Cancelled && value != ProcessingState.Cancelled) {
      return
    }
    state = value
    if (registered) {
      if (job.partialOf.isEmpty) {
        val now = Instant.now
        if (prevState == ProcessingState.NotStarted) {
          info.started = Some(now)
        }
        state match {
          case ProcessingState.Queued =>
            JobStateManager.logQueued(this)
          case ProcessingState.Running =>
            JobStateManager.logRunning(this)
          case ProcessingState.Failed =>
            info.finished = Some(now)
            JobStateManager.logFailed(this)
          case ProcessingState.Finished =>
            info.finished = Some(now)
            if (job.logCollectionInfo && InputSpec.isCollectionBased(conf.inputSpec)) {
              for (info <- ArchCollectionInfo.get(conf.inputSpec.collectionId)) {
                info.setLastJob(job.id, conf.isSample, now).save()
              }
            }
            JobStateManager.logFinished(this)
          case ProcessingState.Cancelled =>
            info.finished = Some(now)
            info.state = Some(ProcessingState.Cancelled)
            JobStateManager.logCancelled(this)
        }
        if (job.generatesOutput) info.save(outPath)
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
          case ProcessingState.Cancelled =>
            JobStateManager.logCancelled(this, subJob = true)
        }
      }
      for (func <- _onStateChanged) func()
    }
  }

  def stateStr: String = ProcessingState.Strings(state)
  def sampleVizData: Option[SampleVizData] = job.sampleVizData(conf)
  def templateVariables: Seq[(String, Any)] = job.templateVariables(conf)
  def outFiles: Iterator[DerivativeOutput] = UuidManager.outFilesCache(uuid).map(_.files).getOrElse(job.outFiles(conf))
  def lazyOutFilesCache: Option[Future[DerivativeOutputCache]] = UuidManager.lazyOutFilesCache(this)
  def lazyOutFiles: Option[Future[Iterator[DerivativeOutput]]] = lazyOutFilesCache.map { future =>
    future.value.flatMap(_.toOption).map(_.files).map(Future.successful).getOrElse(future.map(_.files))
  }

  private var _onStateChanged: Seq[() => Unit] = Seq.empty
  def onStateChanged(action: => Unit): Unit = _onStateChanged :+= (() => action)

  private var _onUnregistered: Seq[() => Unit] = Seq.empty
  def onUnregistered(action: => Unit): Unit = _onUnregistered :+= (() => action)

  def unregistered(): Unit = for (func <- _onUnregistered) func()

  def isResumable: Boolean = inputRevision == conf.inputSpec.revision && job.isResumable(conf)

  def stopJob(): Boolean = {
    if (state < ProcessingState.Finished) {
      if (state == ProcessingState.Queued) {
        val activeChild = active
        if (activeChild != this) {
          // ChainedJob parent: the parent is never in a JobQueue itself — only the child is.
          // Set parent to Cancelled first so the child's onStateChanged/onUnregistered callbacks
          // don't race and overwrite it back to Failed, then stop the actual queued child.
          updateState(ProcessingState.Cancelled)
          activeChild.stopJob()
          // JobManager.unregister(this) will be called via child.onUnregistered callback
        } else {
          // Regular job: directly queued in a JobQueue
          for (q <- queue) {
            q.dequeue(this)
            unsetQueue()
          }
          // Update state to cancelled (while still registered so it logs)
          if (registered) {
            updateState(ProcessingState.Cancelled)
            JobManager.unregister(this)
          }
        }
        true
      } else {
        // For running jobs, set state to Cancelled FIRST to prevent race condition
        // with Future completion handler in JobManagerBase
        updateState(ProcessingState.Cancelled)
        // Then call job.stop() to cancel the actual execution (e.g., Spark job group)
        job.stop(this)
      }
    } else true
  }
}

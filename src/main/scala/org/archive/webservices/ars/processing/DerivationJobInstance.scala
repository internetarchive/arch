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

  private var activeStage: Option[DerivationJobInstance] = None

  def setStage(instance: DerivationJobInstance): Unit = {
    activeStage = Some(instance)
    JobStateManager.updateRunning(this)
  }

  def unsetStage(): Unit = activeStage = None

  def active: DerivationJobInstance = activeStage.getOrElse(this)

  def outPath: String = conf.outputPath + job.relativeOutPath

  def info: ArchJobInstanceInfo = {
    if (job.generatesOuputput) {
      val info = ArchJobInstanceInfo(outPath)
      info.conf = Some(conf)
      info
    } else ArchJobInstanceInfo.inMemory
  }

  def updateState(value: Int): Unit = {
    val prevState = state
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
        }
        if (job.generatesOuputput) info.save(outPath)
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
    if (state < ProcessingState.Finished) job.stop(this) else true
  }
}

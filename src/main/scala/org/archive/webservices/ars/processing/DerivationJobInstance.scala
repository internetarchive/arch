package org.archive.webservices.ars.processing

import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model._
import org.archive.webservices.ars.util.UUID
import org.archive.webservices.sparkling.io.HdfsIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DerivationJobInstance {
  def uuid: String = uuid(false)

  def uuid(reserve: Boolean): String = {
    var uuid = UUID.uuid7str
    for (path <- ArchConf.uuidJobOutPath) {
      val uuidPath = new Path(path + "/" + uuid)
      while (HdfsIO.fs.exists(uuidPath)) uuid = UUID.uuid7str
      if (reserve) HdfsIO.fs.mkdirs(uuidPath)
    }
    uuid
  }
}

case class DerivationJobInstance(job: DerivationJob, conf: DerivationJobConf) {
  var predefUuid: Option[String] = None

  lazy val uuid: String = {
    val uuid = predefUuid.orElse(info.uuid).getOrElse(DerivationJobInstance.uuid)
    info.uuid = Some(uuid)
    uuid
  }

  var registered = false
  var state: Int = ProcessingState.NotStarted

  var user: Option[ArchUser] = None

  lazy val inputSize: Long = job.inputSize(conf)

  lazy val outputSize: Long = job.outputSize(conf)

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

  def queueIndex: Int = {
    _queue
      .map { q =>
        if (queuePos >= q.pos) queuePos - q.pos else queuePos + (Int.MaxValue - q.pos)
      }
      .getOrElse(-1)
  }

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
  def outFiles: Iterator[DerivativeOutput] = job.outFilesCached(conf)
  def lazyOutFilesCache: Option[Future[DerivativeOutputCache]] = job.lazyOutFilesCache(conf)
  def lazyOutFiles: Option[Future[Iterator[DerivativeOutput]]] = lazyOutFilesCache.map { future =>
    future.map(_.files)
  }

  private var _onStateChanged: Seq[() => Unit] = Seq.empty
  def onStateChanged(action: => Unit): Unit = _onStateChanged :+= (() => action)

  private var _onUnregistered: Seq[() => Unit] = Seq.empty
  def onUnregistered(action: => Unit): Unit = _onUnregistered :+= (() => action)

  def unregistered(): Unit = for (func <- _onUnregistered) func()
}

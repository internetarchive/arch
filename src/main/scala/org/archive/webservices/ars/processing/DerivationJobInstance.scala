package org.archive.webservices.ars.processing

import java.time.Instant

import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{
  ArchCollectionInfo,
  ArchJobInstanceInfo,
  DerivativeOutput
}
import org.archive.webservices.ars.util.MailUtil

case class DerivationJobInstance(job: DerivationJob, conf: DerivationJobConf) {
  var user: Option[ArchUser] = None

  var state: Int = ProcessingState.NotStarted

  private var activeStage: Option[DerivationJobInstance] = None

  private var _queue: Option[JobQueue] = None
  private var queuePos: Int = -1

  def unsetQueue(): Unit = {
    _queue = None
    queuePos = -1
  }

  def setQueue(queue: JobQueue, pos: Int): Unit = {
    _queue = Some(queue)
    queuePos = pos
  }

  def queue: Option[JobQueue] = _queue

  def queueIndex: Int =
    _queue
      .map { q =>
        if (queuePos >= q.pos) queuePos - q.pos else queuePos + (Int.MaxValue - q.pos)
      }
      .getOrElse(-1)

  def setStage(instance: DerivationJobInstance): Unit = activeStage = Some(instance)

  def unsetStage(): Unit = activeStage = None

  def active: DerivationJobInstance = activeStage.getOrElse(this)

  def info: ArchJobInstanceInfo = ArchJobInstanceInfo.get(conf.outputPath + "/" + job.id)

  def updateState(value: Int): Unit = {
    val prevState = state
    state = value
    for (func <- _onStateChanged) func()
    if (job.partialOf.isEmpty && state > prevState) {
      val now = Instant.now
      var info = this.info
      if (prevState == ProcessingState.NotStarted) {
        info = info.setStartTime(now)
      }
      if (state == ProcessingState.Finished) {
        info = info.setFinishedTime(now)
        val nameSuffix = if (conf.sample < 0) "" else " (Sample)"
        ArchCollectionInfo
          .get(conf.collectionId)
          .setLastJob(job.name + nameSuffix, now)
          .save()
        for (email <- user.flatMap(_.email)) {
          MailUtil.sendTemplate(
            "finished",
            Map("to" -> email, "jobname" -> job.name, "collection" -> conf.collectionId))
        }
      }
      info.save(conf.outputPath + "/" + job.id)
    }
  }

  def stateStr: String = ProcessingState.Strings(state)
  def templateVariables: Seq[(String, Any)] = job.templateVariables(conf)
  def outFiles: Seq[DerivativeOutput] = job.outFiles(conf)

  private var _onStateChanged: Seq[() => Unit] = Seq.empty
  def onStateChanged(action: => Unit): Unit = _onStateChanged :+= (() => action)
}

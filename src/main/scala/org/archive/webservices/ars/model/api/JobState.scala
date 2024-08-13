package org.archive.webservices.ars.model.api

import org.archive.webservices.ars.processing.{DerivationJobInstance, ProcessingState}
import org.archive.webservices.ars.util.FormatUtil

case class JobState(
    id: String,
    uuid: String,
    name: String,
    sample: Int,
    state: String,
    started: Boolean,
    finished: Boolean,
    failed: Boolean,
    activeStage: String,
    activeState: String,
    queue: Option[String],
    queuePos: Option[
      Integer
    ], // Option type lookup fails for Int in package.scala, so use Integer
    startTime: Option[String],
    finishedTime: Option[String])
    extends ApiResponseObject[JobState]

object JobState {
  def apply(instance: DerivationJobInstance): JobState = {
    val active = instance.active
    val info = instance.info
    JobState(
      id = instance.job.id,
      uuid = instance.uuid,
      name = instance.job.name,
      sample = instance.conf.sample,
      state = instance.stateStr,
      started = (instance.state != ProcessingState.NotStarted),
      finished = (instance.state == ProcessingState.Finished),
      failed = (instance.state == ProcessingState.Failed),
      activeStage = active.job.stage,
      activeState = active.stateStr,
      queue = active.queue.map(_.name),
      queuePos = active.queue.map(q => active.queueIndex),
      startTime = info.started.map(FormatUtil.instantTimeString),
      finishedTime = info.finished.map(FormatUtil.instantTimeString))
  }
}

package org.archive.webservices.ars.processing

object ProcessingState {
  val Strings: Seq[String] = Seq("Not started", "Queued", "Running", "Finished", "Failed")

  val NotStarted = 0
  val Queued = 1
  val Running = 2
  val Finished = 3
  val Failed = 4
}

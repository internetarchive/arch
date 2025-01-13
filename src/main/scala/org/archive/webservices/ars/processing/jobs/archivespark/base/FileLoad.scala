package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.archive.webservices.archivespark.model.dataloads.DataLoad

object FileLoad extends DataLoad[String] {
  trait Root extends DataLoadRoot
}

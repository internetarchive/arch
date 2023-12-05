package org.archive.webservices.ars.processing.jobs.archivespark

import org.archive.webservices.archivespark.model.dataloads.DataLoad

object PlainTextLoad extends DataLoad[String] {
  trait Root extends DataLoadRoot
}
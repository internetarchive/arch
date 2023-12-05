package org.archive.webservices.ars.processing.jobs.archivespark

import org.archive.webservices.archivespark.functions.Entities
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}

object ArchiveSparkEntitiesExtraction extends ArchiveSparkTextLoadJob {
  val name: String = id
  val description: String = "ArchiveSpark job " + name
  val uuid: String = "01234567-1234-5678-9012-" + id.take(12)
  val category: ArchJobCategory = ArchJobCategories.None

  val functions = Seq(Entities.of(textLoad))
}

package org.archive.webservices.ars.processing.jobs.archivespark.preset

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.AiJob
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchEnrichRoot
import org.archive.webservices.ars.processing.jobs.archivespark.functions.{PySmokeTest, Whisper}

object PySmokeTestJob extends AiJob {
  val uuid: String = "01997ab5-83be-7041-9c24-9fc3e30123b3"

  val name: String = "Smoke test"
  val description: String = "An ArchiveSpark with Python test job."

  override val category: ArchJobCategory = ArchJobCategories.None

  override def mimePrefilter(conf: DerivationJobConf): String => Boolean = _ => true

  def functions(conf: DerivationJobConf): Seq[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, _, _]] = {
    Seq(PySmokeTest.noParams)
  }
}

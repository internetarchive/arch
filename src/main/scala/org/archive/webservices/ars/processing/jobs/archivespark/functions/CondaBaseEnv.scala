package org.archive.webservices.ars.processing.jobs.archivespark.functions

trait CondaBaseEnv extends CondaEnv {
  override def condaPackageSourceDir: String = "conda-envs"
}

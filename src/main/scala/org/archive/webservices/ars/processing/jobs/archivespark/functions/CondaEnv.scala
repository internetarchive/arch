package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.sparkling.io.SystemProcess

trait CondaEnv extends ExternalDependencies {
  def condaEnv: String
  def condaPackage: String = condaEnv + ".tar.gz"
  def condaPackageSourceDir: String = this.dataDir

  abstract override def init(shell: SystemProcess): Unit = {
    val condaActivate = s"$condaEnv/bin/activate"

    def activate(): Unit = if (!envLoaded(PythonEnv, loading = true)) {
      shell.exec(s"source $condaActivate", blocking = true)
    }

    make(condaActivate) { _ =>
      val f = ensureFile(condaPackage, condaPackageSourceDir)
      make(condaEnv)(_.mkdirs())
      shell.exec(s"tar -xzf $condaPackage -C $condaEnv", blocking = true)
      f.delete()
      activate()
      shell.exec("conda-unpack", blocking = true)
    }

    activate()

    super.init(shell)
  }
}

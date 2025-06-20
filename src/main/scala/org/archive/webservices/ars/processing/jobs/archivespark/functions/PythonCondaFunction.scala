package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.sparkling.io.SystemProcess

abstract class PythonCondaFunction[A] extends PythonFunction[A] {
  def envName: String

  def initVenv(shell: SystemProcess): Unit = {
    val condaActivate = s"$envName/bin/activate"

    make(condaActivate) { _ =>
      val condaFile = envName + ".tar.gz"
      val f = ensureFile(condaFile)
      make(envName) { dir =>
        dir.mkdir()
      }
      shell.exec(s"tar -xzf $condaFile -C $envName", blocking = true)
      f.delete()
    }

    shell.exec(s"source $condaActivate", blocking = true)
    shell.exec("conda-unpack", blocking = true)
  }
}

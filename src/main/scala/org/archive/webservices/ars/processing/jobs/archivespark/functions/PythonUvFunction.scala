package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.io.SystemProcess

abstract class PythonUvFunction[A] extends PythonFunction[A] {
  private val VenvDir = ".venv"
  private val ProjectFile = "pyproject.toml"

  def initVenv(shell: SystemProcess): Unit = {
    ensureFile(ProjectFile)

    make(VenvDir) { _ =>
      shell.exec("uv venv", blocking = true)
      shell.exec("uv pip install ." + {
        ArchConf.pypiIndexUrl.map(u => s" --index-url $u").getOrElse("")
      })
    }

    shell.exec(s"source $VenvDir/bin/activate")
  }
}

package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.io.{IOUtil, SystemProcess}

trait UVenv extends ExternalDependencies {
  private val VenvDir = ".venv"
  private val ProjectFile = "pyproject.toml"

  val UvDir = ".uv"

  abstract override def init(shell: SystemProcess): Unit = {
    make(UvDir) { _ =>
      ensureFile(ProjectFile, dataDir, target = Some(IOUtil.concatPaths(UvDir, ProjectFile)))
    }

    val venvDir = IOUtil.concatPaths(UvDir, VenvDir)
    make(venvDir) { _ =>
      shell.exec(s"cd $UvDir")
      shell.exec("uv venv", blocking = true)
      shell.exec("uv pip install ." + {
        ArchConf.pypiIndexUrl.map(u => s" --index-url $u").getOrElse("")
      }, blocking = true)
      shell.exec(s"cd ..")
    }

    shell.exec(s"source $venvDir/bin/activate")

    super.init(shell)
  }
}

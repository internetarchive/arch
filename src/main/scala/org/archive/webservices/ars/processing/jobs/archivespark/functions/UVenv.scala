package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.io.{IOUtil, SystemProcess}

import java.io.File

trait UVenv extends ExternalDependencies {
  private val VenvDir = ".venv"
  private val ProjectFile = "pyproject.toml"

  val UvDir = ".uv"
  val UvCacheDir = ".uv-cache"

  abstract override def init(shell: SystemProcess): Unit = {
    make(UvDir) { _ =>
      ensureFile(ProjectFile, dataDir, target = Some(IOUtil.concatPaths(UvDir, ProjectFile)))
    }

    val uvCacheDir = ArchConf.hadoopNodeLocalTempPath + "/" + UvCacheDir
    shell.exec(s"export UV_CACHE_DIR=$uvCacheDir")

    val venvDir = IOUtil.concatPaths(UvDir, VenvDir)
    make(venvDir) { _ =>
      shell.exec(s"cd $UvDir")
      if (!envLoaded(PythonEnv)) shell.exec("uv venv", blocking = true)
      else new File(venvDir).mkdir() // flag dir to skip this 'make' next time
      shell.exec(s"uv pip install ." + {
        ArchConf.pypiIndexUrl.map(u => s" --index-url $u").getOrElse("")
      }, blocking = true)
      shell.exec(s"cd ..")
    }

    if (!envLoaded(PythonEnv, loading = true)) {
      shell.exec(s"source $venvDir/bin/activate")
    }

    super.init(shell)
  }
}

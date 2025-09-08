package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil, SystemProcess}

import java.io.{File, InputStream}
import java.net.URL

trait ExternalDependencies {
  def dataDir: String
  def workingDir: String = ArchConf.hadoopNodeLocalTempPath + "/" + dataDir

  def init(shell: SystemProcess): Unit

  def ensureFile(file: String, sourceDir: String = dataDir, target: Option[String] = None): File = {
    val dst = target.getOrElse(file)
    make(dst) { _ =>
      copyFile(IOUtil.concatPaths(sourceDir, file), dst)
    }
  }

  def copyFile(src: String, dst: String): String = {
    val hdfs = ArchConf.hdfsJobArtifactPath.map(IOUtil.concatPaths(_, src)).filter(HdfsIO.exists)
    hdfs match {
      case Some(src) => copyFromHdfs(src, dst)
      case None =>
        val url = ArchConf.jobArtifactUrl.stripSuffix("/") + "/" + src
        copyFromUrl(url, dst)
    }
  }

  def make(file: String)(make: File => Unit): File = {
    val f = new File(workingDir, file)
    if (f.exists()) return f
    make(f)
    f
  }

  def copyFile(in: InputStream, file: String): String = {
    try {
      val localFile = workingDir + "/" + file
      val f = new File(localFile)
      f.getParentFile.mkdirs()
      if (f.exists()) f.delete()
      val tmpFile = localFile + "._downloading"
      val out = IOUtil.fileOut(tmpFile)
      try {
        IOUtil.copy(in, out)
      } finally out.close()
      new File(tmpFile).renameTo(f)
      localFile
    } finally in.close()
  }

  def copyFromHdfs(src: String, dst: String): String = {
    copyFile(HdfsIO.open(src, decompress = false), dst)
  }

  def copyFromUrl(url: String, dst: String): String = {
    copyFile(new URL(url).openStream, dst)
  }
}

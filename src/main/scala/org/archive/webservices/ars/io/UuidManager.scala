package org.archive.webservices.ars.io

import io.circe.parser.parse
import io.circe.syntax._
import org.apache.hadoop.fs.Path
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.model.{ArchConf, DerivativeOutput, DerivativeOutputCache}
import org.archive.webservices.ars.processing.jobs.system.DummyDerivationJob
import org.archive.webservices.ars.processing.{DerivationJobConf, DerivationJobInstance, JobManager}
import org.archive.webservices.ars.util.{LazyCache, UUID}
import org.archive.webservices.sparkling.io.HdfsIO

import scala.collection.immutable.ListMap
import scala.concurrent.Future

object UuidManager {
  val InstanceUuidFileSuffix = ".uuid.json"
  val OutFilesCacheFile = "outfiles.cached.jsonl"

  def uuid: String = uuid(false)

  def uuid(reserve: Boolean): String = {
    var uuid = UUID.uuid7str
    for (path <- ArchConf.uuidJobOutPath) {
      val uuidPath = new Path(path + "/" + uuid)
      while (HdfsIO.fs.exists(uuidPath)) uuid = UUID.uuid7str
      if (reserve) HdfsIO.fs.mkdirs(uuidPath)
    }
    uuid
  }

  def uuidPath(uuid: String): Option[String] = {
    ArchConf.uuidJobOutPath.map(IOHelper.concatPaths(_, uuid))
  }

  def infoFile(uuid: String): Option[String] = {
    uuidPath(uuid).map(IOHelper.concatPaths(_, uuid + InstanceUuidFileSuffix))
  }

  def saveUuidInfo(instance: DerivationJobInstance): Unit = {
    for (f <- infoFile(instance.uuid)) {
      HdfsIO.writeLines(
        f,
        Seq(
          (ListMap(
            "jobUuid" -> instance.job.uuid.asJson,
            "jobId" -> instance.job.id.asJson,
            "revision" -> instance.job.revision.asJson,
            "inputRevision" -> instance.conf.inputSpec.revision.asJson,
            "conf" -> instance.conf.toJson) ++ {
            instance.user.map("user" -> _.id.asJson)
          }).asJson.spaces4),
        overwrite = true)
    }
  }

  def finalizeUuid(instance: DerivationJobInstance): Unit = {
    writeOutFilesCache(instance)
  }

  def getInstance(uuid: String): Option[DerivationJobInstance] = {
    infoFile(uuid).filter(HdfsIO.exists).flatMap { uuidFilePath =>
      parse(HdfsIO.lines(uuidFilePath).mkString).toOption.map(_.hcursor).flatMap { cursor =>
        for {
          conf <- cursor.downField("conf").focus.flatMap(DerivationJobConf.fromJson)
        } yield {
          val revision = cursor.get[Int]("revision").getOrElse(0)
          val inputRevision = cursor.get[Int]("inputRevision").getOrElse(0)
          val instance = cursor.get[String]("jobUuid").toOption.flatMap(JobManager.uuidLookup.get).filter { job =>
            job.revision == revision
          }.flatMap { job =>
            Option(job.history(conf)).filter(instance => HdfsIO.exists(instance.outPath))
          }.getOrElse(DummyDerivationJob.history(conf))
          instance.predefUuid = Some(uuid)
          instance.inputRevision = inputRevision
          instance.user = cursor.get[String]("user").toOption.flatMap(ArchUser.get)
          instance
        }
      }
    }
  }

  def outFilesCacheFile(path: String): String = IOHelper.concatPaths(path, OutFilesCacheFile)

  def outFilesCache(uuid: String): Option[DerivativeOutputCache] = outFilesCache(uuidPath(uuid))

  def outFilesCache(conf: DerivationJobConf): Option[DerivativeOutputCache] = outFilesCache(Some(conf.outputPath))

  private def outFilesCache(uuidPath: Option[String]): Option[DerivativeOutputCache] = {
    uuidPath.map(outFilesCacheFile).flatMap {
      LazyCache.getIfCached(_)(DerivativeOutputCache.parse)
    }
  }

  def lazyOutFilesCache(instance: DerivationJobInstance): Option[Future[DerivativeOutputCache]] = {
    val path = uuidPath(instance.uuid)
    outFilesCache(path).map(Future.successful).orElse {
      path.map(outFilesCacheFile).map { f =>
        LazyCache.getOrCache(f)(
          DerivativeOutputCache.parse,
          DerivativeOutputCache.write(instance.outFiles, _))
      }
    }
  }

  def writeOutFilesCache(instance: DerivationJobInstance, globMimeType: Option[(String, String, String)] = None): Option[Future[Boolean]] = {
    uuidPath(instance.uuid).map(outFilesCacheFile).map { cacheFile =>
      val outFiles = globMimeType.map { case (glob, mimeType, fileType) =>
        HdfsIO.files(IOHelper.concatPaths(instance.conf.outputPath, glob)).map { file =>
          val (path, name) = file.splitAt(file.lastIndexOf('/'))
          DerivativeOutput(name.stripPrefix("/"), path, fileType, mimeType)
        }
      }.getOrElse(instance.job.outFiles(instance.conf))
      LazyCache.overwriteCache(cacheFile, DerivativeOutputCache.write(outFiles, _))
    }
  }
}

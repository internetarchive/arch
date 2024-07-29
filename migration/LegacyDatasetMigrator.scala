import scala.util.matching.Regex

import org.apache.hadoop.fs.{FileUtil, Path}
import org.apache.spark.deploy.SparkHadoopUtil
import io.circe.parser.parse

import org.archive.webservices.sparkling.io.HdfsIO

import org.archive.webservices.ars.model.{
  ArchCollection,
  ArchConf,
  ArchJobCategories,
  ArchJobInstanceInfo
}
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.processing.{
  DerivationJobConf,
  DerivationJobInstance,
  JobManager
}

object LegacyDatasetMigrator {

  val globalGlob =
    s"${ArchConf.globalJobOutPath}/*/{out,samples}/*/${ArchJobInstanceInfo.InfoFile}"

  val usersGlob = s"${ArchConf.jobOutPath}/*/*/{out,samples}/*/${ArchJobInstanceInfo.InfoFile}"

  val pathRegex = new Regex(
    s"^.+/[^/]*/([^/].*)/([^/].*)/(out|samples)/([^/]*)/${ArchJobInstanceInfo.InfoFile}$$",
    "userId",
    "collectionId",
    "outOrSamples",
    "jobId")

  val migratedMarkerFile = "migrated-to-uuid"

  val globalDatasetUsername = "arch:__global__"

  def info(msg: String) = println(s"[INFO] ${msg}")
  def warn(msg: String) = println(s"[WARN] ${msg}")

  def isMigrated(instance: DerivationJobInstance): Boolean = {
    HdfsIO.exists(s"${instance.outPath}/${migratedMarkerFile}")
  }

  def markMigrated(instance: DerivationJobInstance, uuid: String) = {
    HdfsIO.writeLines(s"${instance.outPath}/${migratedMarkerFile}", Seq(uuid), overwrite = false)
    None
  }

  def copyDatasetFiles(oldInstance: DerivationJobInstance, newInstance: DerivationJobInstance) = {
    val newOutPath = new Path(newInstance.conf.outputPath)
    // Create the output path.
    HdfsIO.fs.mkdirs(newOutPath)
    // Copy the dataset files.
    FileUtil.copy(
      HdfsIO.fs,
      new Path(oldInstance.conf.outputPath + oldInstance.job.relativeOutPath),
      HdfsIO.fs,
      newOutPath,
      false,
      true,
      new org.apache.hadoop.conf.Configuration(SparkHadoopUtil.get.conf)
    )
    // Overwrite the old/copied info.json with a new one.
    val instanceInfo = ArchJobInstanceInfo.apply(newInstance.conf.outputPath)
    instanceInfo.uuid = Some(newInstance.uuid)
    instanceInfo.conf = Some(newInstance.conf)
    instanceInfo.started = oldInstance.info.started
    instanceInfo.finished = oldInstance.info.finished
    instanceInfo.save(newInstance.conf.outputPath + newInstance.job.relativeOutPath)
  }

  def migrateLegacyDataset(
      user: ArchUser,
      collection: ArchCollection,
      instance: DerivationJobInstance) = {

    // Reserve an output job UUID.
    val uuid = DerivationJobInstance.uuid(reserve = true)

    // Create a job configuration and instance.
    val conf = DerivationJobConf
      .fromJson(
        parse(s"""
{
  "inputSpec": {
    "type": "collection",
    "collectionId": "${collection.sourceId}"
  },
  "params": {
    "dataset": "${instance.job.id}"
  }
}
      """).toOption.get.hcursor,
        instance.conf.isSample,
        ArchConf.uuidJobOutPath.map(_ + "/" + uuid))
      .get

    // Register the instance to create the uuid json file.
    val newInstance = DerivationJobInstance(instance.job, conf)
    newInstance.predefUuid = Some(uuid);
    newInstance.user = Some(user)
    JobManager.registerUuid(newInstance)

    // Copy the dataset files.
    copyDatasetFiles(instance, newInstance)

    markMigrated(instance, uuid)

    info(s"Migrated: ${instance.outPath} to ${newInstance.conf.outputPath}")
  }

  def run() = {
    for ((glob, isGlobal) <- Seq((globalGlob, true), (usersGlob, false))) {
      for {
        p <- HdfsIO.files(glob, recursive = false)
        Seq(userId, collectionId, outOrSamples, jobId) <- pathRegex.findFirstMatchIn(p).map(_.subgroups)
        if !collectionId.endsWith("_backup")
      } {
        val Some(user) = ArchUser.get(
          if (isGlobal) globalDatasetUsername else userId.split("-", 2).mkString(":"))
        val userCollectionId = ArchCollection.userCollectionId(collectionId, user)
        ArchCollection.get(userCollectionId) match {
          case None => warn(s"Could not retrieve collection: ${userCollectionId}")
          case Some(collection) => {
            val Some(instance) = JobManager
              .getInstance(
                jobId,
                DerivationJobConf.collection(
                  collection,
                  sample = outOrSamples == "samples",
                  global = isGlobal))
            // Ignore System-type jobs and already-migrated datasets.
            if (instance.job.category != ArchJobCategories.System && !isMigrated(instance)) {
              migrateLegacyDataset(user, collection, instance)
            }
          }
        }
      }
    }
  }
}

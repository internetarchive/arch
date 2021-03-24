package org.archive.webservices.ars.processing.jobs.shared

import io.archivesunleashed.ArchiveRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.desc
import org.archive.helge.sparkling.io.HdfsIO
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.ars.model.{ArsCloudJobCategories, ArsCloudJobCategory}
import org.archive.webservices.ars.processing.{DerivationJobConf, ProcessingState}

abstract class BinaryInformationAutJob extends AutJob {
  val category: ArsCloudJobCategory = ArsCloudJobCategories.BinaryInformation

  val mimeTypeCountFile: String = "mime-type-count.csv.gz"

  override def runSpark(rdd: RDD[ArchiveRecord], outPath: String): Unit = {
    val data = df(rdd).cache

    data.write
      .option("timestampFormat", "yyyy/MM/dd HH:mm:ss ZZ")
      .format("csv")
      .csv(outPath + "/_" + targetFile)

    data
      .groupBy("mime_type_web_server")
      .count()
      .orderBy(desc("count"))
      .limit(5)
      .write
      .option("timestampFormat", "yyyy/MM/dd HH:mm:ss ZZ")
      .format("csv")
      .csv(outPath + "/_" + mimeTypeCountFile)
  }

  override def checkSparkState(outPath: String): Option[Int] =
    super.checkSparkState(outPath).map { state =>
      if (!HdfsIO.exists(outPath + "/_" + mimeTypeCountFile + "/_SUCCESS")) ProcessingState.Failed
      else state
    }

  override def postProcess(outPath: String): Boolean = super.postProcess(outPath) && {
    IOHelper.concatLocal(
      outPath + "/_" + mimeTypeCountFile,
      mimeTypeCountFile,
      _.startsWith("part-"),
      compress = true,
      deleteSrcFiles = true,
      deleteSrcPath = true) { tmpFile =>
      val outFile = outPath + "/" + mimeTypeCountFile
      HdfsIO.copyFromLocal(tmpFile, outFile, move = true, overwrite = true)
      HdfsIO.exists(outFile)
    }
  }

  override def checkFinishedState(outPath: String): Option[Int] =
    super.checkFinishedState(outPath).map { state =>
      if (!HdfsIO.exists(outPath + "/" + mimeTypeCountFile)) ProcessingState.Failed
      else state
    }

  override def templateName: Option[String] = Some("jobs/BinaryInformationExtraction")

  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    val mimeCount = HdfsIO
      .lines(conf.outputPath + relativeOutPath + "/" + mimeTypeCountFile)
      .map(_.split(','))
      .map {
        case Array(mimeType, count) =>
          (mimeType, count.toInt)
      }
    super.templateVariables(conf) ++ Seq("mimeCount" -> mimeCount)
  }
}

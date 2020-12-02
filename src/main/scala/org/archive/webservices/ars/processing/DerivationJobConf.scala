package org.archive.webservices.ars.processing

import org.archive.webservices.ars.model.ArsCloudConf

case class DerivationJobConf (
  collectionId: String,
  inputPath: String,
  outputPath: String
)

object DerivationJobConf {
  def collection(collectionId: String): Option[DerivationJobConf] = {
    if (collectionId.startsWith("ARCHIVEIT-")) Some(aitCollection(collectionId))
    else None
  }

  def aitCollection(collectionId: String): DerivationJobConf = {
    val aitId = collectionId.stripPrefix("ARCHIVEIT-")
    val inputPath = ArsCloudConf.aitCollectionPath + s"/$aitId/" + ArsCloudConf.aitCollectionWarcDir + "/*.warc.gz"
    val outputPath = ArsCloudConf.jobOutPath + s"/$collectionId/out"
    DerivationJobConf(collectionId, inputPath, outputPath)
  }
}
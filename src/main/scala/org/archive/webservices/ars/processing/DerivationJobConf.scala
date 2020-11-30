package org.archive.webservices.ars.processing

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
    val inputPath = s"/search/ait/$aitId/arcs/*.warc.gz"
    val outputPath = s"/user/helge/ars-cloud/$collectionId/out"
    DerivationJobConf(collectionId, inputPath, outputPath)
  }
}
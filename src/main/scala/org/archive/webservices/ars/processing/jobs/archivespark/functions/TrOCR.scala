package org.archive.webservices.ars.processing.jobs.archivespark.functions

object TrOCR extends CondaBasedFunction[String] {
  override val hdfsDataDir: String = "trocr"
  override val condaEnv: String = "conda-trocr-env"
  override val pythonFile: String = "trocr-run.py"
  override val additionalPackages: Seq[String] = Seq("trocr-models.tar.gz", "craft-pytorch.tar.gz")
  override val pythonArgumentFiles: Seq[String] = Seq("trocr-base-handwritten", "weights/craft_mlt_25k.pth", "weights/craft_refiner_CTW1500.pth")

  override def processOutput(output: String): Option[String] = Some(output)
}

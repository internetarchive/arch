package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.PythonFunctionAdapter

object TrOCR extends PythonFunctionAdapter[String] {
  override def func: PythonFunction[String] = new TrOCR
}

class TrOCR extends PythonFunction[String] with UVenv with CondaBaseEnv {
  override val label: String = "trocr"
  override val dataDir: String = s"$label/20240807195100"
  override val pythonFile: String = s"$label-run.py"
  override val additionalPackages: Seq[String] =
    Seq(s"$label-models.tar.gz", "craft-pytorch.tar.gz")
  override val pythonArgumentFiles: Seq[String] = Seq(
    s"$label-base-handwritten",
    "weights/craft_mlt_25k.pth",
    "weights/craft_refiner_CTW1500.pth")

  override def condaEnv: String = "pytorch-1.11.0"

  override def processOutput(output: String): Option[String] = Some(output)
}

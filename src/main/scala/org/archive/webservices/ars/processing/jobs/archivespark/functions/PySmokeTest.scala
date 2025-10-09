package org.archive.webservices.ars.processing.jobs.archivespark.functions

import org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters.PythonFunctionAdapter

object PySmokeTest extends PythonFunctionAdapter[String] {
  override def func: PythonFunction[String] = new PySmokeTest
}

class PySmokeTest extends PythonFunction[String] {
  override val label: String = "smoke-test"
  override val dataDir: String = label
  override val pythonFile: String = s"test-run.py"

  override def pythonArgumentFiles: Seq[String] = Seq.empty

  override def processOutput(output: String): Option[String] = Some(s" >>> $output <<< ")
}

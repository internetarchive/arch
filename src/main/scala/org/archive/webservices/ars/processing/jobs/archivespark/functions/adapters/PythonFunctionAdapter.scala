package org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.functions.PythonFunction

trait PythonFunctionAdapter[T] extends ArchArchiveSparkFunctionAdapter[String, T] {
  def func: PythonFunction[T]

  override def baseFunc: EnrichFunc[_, _, String, T] = func

  override def initFunc(params: DerivationJobParameters): EnrichFunc[_, _, String, T] = {
    val f = func
    f.initFunc(params)
    f
  }
}

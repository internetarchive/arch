package org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.functions.CondaBasedFunction

trait CondaBasedArchiveSparkFunctionAdapter[Source]
    extends ArchArchiveSparkFunctionAdapter[Source] {
  def func: CondaBasedFunction[Source]

  override def baseFunc: EnrichFunc[_, Source, _] = func.asInstanceOf[EnrichFunc[_, Source, _]]

  override def initFunc(params: DerivationJobParameters): EnrichFunc[_, Source, _] = {
    val f = func
    f.initFunc(params)
    f.asInstanceOf[EnrichFunc[_, Source, _]]
  }
}

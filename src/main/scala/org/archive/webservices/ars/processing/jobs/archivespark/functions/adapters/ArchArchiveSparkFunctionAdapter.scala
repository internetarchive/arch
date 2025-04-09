package org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters

import org.archive.webservices.archivespark.model.{EnrichFunc, EnrichRoot}
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchEnrichRoot

import scala.util.Try

trait ArchArchiveSparkFunctionAdapter[Source, T] {
  def name: String = baseFunc.getClass.getSimpleName.stripSuffix("$")
  def baseFunc: EnrichFunc[_, _, Source, T]
  def defaultDependency: Option[FieldPointer[ArchEnrichRoot[_], Source]] = None
  def noParams(on: Option[FieldPointer[ArchEnrichRoot[_], Source]])
  : EnrichFunc[ArchEnrichRoot[_], EnrichRoot, Source, T] = {
    val dependency = on.orElse(defaultDependency)
    dependency
      .map(baseFunc.on(_))
      .getOrElse(baseFunc)
      .asInstanceOf[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, Source, T]]
  }
  def noParams: EnrichFunc[ArchEnrichRoot[_], EnrichRoot, Source, T] = noParams(None)
  def withParams(
                  params: DerivationJobParameters,
                  on: Option[FieldPointer[ArchEnrichRoot[_], Source]] = None)
  : EnrichFunc[ArchEnrichRoot[_], EnrichRoot, Source, T] = {
    if (params.isEmpty) noParams(on)
    else {
      val dependency = on.orElse(defaultDependency)
      val func = initFunc(params)
      dependency
        .map(func.on)
        .getOrElse(func)
        .asInstanceOf[EnrichFunc[ArchEnrichRoot[_], EnrichRoot, Source, T]]
    }
  }
  def initFunc(params: DerivationJobParameters): EnrichFunc[_, _, Source, T] = baseFunc
  def toDependencyPointer(func: EnrichFunc[ArchEnrichRoot[_], _, _, _])
  : Option[FieldPointer[ArchEnrichRoot[_], Source]] = Try {
    func.asInstanceOf[FieldPointer[ArchEnrichRoot[_], Source]]
  }.toOption
}



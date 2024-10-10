package org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters

import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.archivespark.model.pointers.FieldPointer
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchEnrichRoot

import scala.util.Try

trait ArchArchiveSparkFunctionAdapter[Source] {
  def name: String = baseFunc.getClass.getSimpleName.stripSuffix("$")
  def baseFunc: EnrichFunc[_, Source, _]
  def defaultDependency: Option[FieldPointer[ArchEnrichRoot[_], Source]] = None
  def noParams(on: Option[FieldPointer[ArchEnrichRoot[_], Source]])
      : EnrichFunc[ArchEnrichRoot[_], Source, _] = {
    val dependency = on.orElse(defaultDependency)
    dependency
      .map(baseFunc.on(_))
      .getOrElse(baseFunc)
      .asInstanceOf[EnrichFunc[ArchEnrichRoot[_], Source, _]]
  }
  def noParams: EnrichFunc[ArchEnrichRoot[_], Source, _] = noParams(None)
  def withParams(
      params: DerivationJobParameters,
      on: Option[FieldPointer[ArchEnrichRoot[_], Source]] = None)
      : EnrichFunc[ArchEnrichRoot[_], Source, _] = {
    if (params.isEmpty) noParams(on)
    else {
      val dependency = on.orElse(defaultDependency)
      val func = initFunc(params)
      dependency
        .map(func.on)
        .getOrElse(func)
        .asInstanceOf[EnrichFunc[ArchEnrichRoot[_], Source, _]]
    }
  }
  def initFunc(params: DerivationJobParameters): EnrichFunc[_, Source, _] = baseFunc
  def toDependencyPointer(func: EnrichFunc[ArchEnrichRoot[_], _, _])
      : Option[FieldPointer[ArchEnrichRoot[_], Source]] = Try {
    func.asInstanceOf[FieldPointer[ArchEnrichRoot[_], Source]]
  }.toOption
}
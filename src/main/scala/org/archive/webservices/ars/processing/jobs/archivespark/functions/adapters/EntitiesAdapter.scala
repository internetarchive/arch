package org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import org.archive.webservices.archivespark.functions.{Entities, EntitiesConstants}
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.archivespark.model.pointers.DataLoadPointer
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.CoreNlpEntities

import java.util.Properties
import scala.collection.JavaConverters.asScalaSetConverter

object EntitiesAdapter extends ArchArchiveSparkFunctionAdapter[String] {
  override lazy val baseFunc: Entities = new CoreNlpEntities()

  override def defaultDependency: Option[DataLoadPointer[ArchEnrichRoot[_], String]] = Some(
    ArchiveSparkEnrichJob.plainTextLoad)

  override def initFunc(params: DerivationJobParameters): EnrichFunc[_, String, _] = {
    val langParam = params.get[String]("lang").map(_.toLowerCase)
    langParam match {
      case Some("chinese") => new CoreNlpEntities(properties(langParam))
      case _ => new CoreNlpEntities(properties(langParam), filterLatin = true)
    }
  }

  def properties(lang: Option[String] = None): Properties = {
    val default = EntitiesConstants.DefaultProps
    lang match {
      case Some(l) =>
        val props = new StanfordCoreNLP(l).getProperties
        for (p <- default.stringPropertyNames.asScala)
          props.setProperty(p, default.getProperty(p))
        props
      case None => default
    }
  }
}

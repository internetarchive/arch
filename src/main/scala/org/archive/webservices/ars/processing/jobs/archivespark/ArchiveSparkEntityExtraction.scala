package org.archive.webservices.ars.processing.jobs.archivespark

import org.archive.webservices.archivespark.functions.{Entities, EntitiesConstants}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchiveSparkTextLoadJob

import java.util.Properties
import scala.collection.JavaConverters.asScalaSetConverter

abstract class ArchiveSparkEntityExtraction extends ArchiveSparkTextLoadJob {
  val name: String = id
  val description: String = "ArchiveSpark job " + name
  val category: ArchJobCategory = ArchJobCategories.None

  def properties(lang: String): Properties = {
    val default = EntitiesConstants.DefaultProps
    val props = new StanfordCoreNLP(lang).getProperties
    for (p <- default.stringPropertyNames.asScala) props.setProperty(p, default.getProperty(p))
    props
  }

  def functions(conf: DerivationJobConf) = {
    val entities = conf.params.get[String]("lang") match {
      case Some(lang) => Entities(properties(lang))
      case None => Entities
    }
    Seq(entities.of(textLoad))
  }
}

object ArchiveSparkEntityExtraction extends ArchiveSparkEntityExtraction {
  override def uuid: String = "018d114d-3426-730e-94a1-b56ca73fc1ad"
}

object ArchiveSparkEntityExtractionChinese extends ArchiveSparkEntityExtraction {
  override def uuid: String = "018d1151-3a3a-7184-b6ed-8ec176ee750e"

  override def properties(lang: String): Properties = super.properties("chinese")
}
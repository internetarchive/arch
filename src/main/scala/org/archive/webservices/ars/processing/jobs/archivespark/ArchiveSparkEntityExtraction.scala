package org.archive.webservices.ars.processing.jobs.archivespark

import edu.stanford.nlp.pipeline.StanfordCoreNLP
import org.archive.webservices.archivespark.functions.{Entities, EntitiesConstants}
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJobConf
import org.archive.webservices.ars.processing.jobs.archivespark.base.ArchiveSparkTextLoadJob

import java.util.Properties
import scala.collection.JavaConverters.asScalaSetConverter

abstract class ArchiveSparkEntityExtraction extends ArchiveSparkTextLoadJob {
  val name: String = id
  val description: String = "ArchiveSpark job " + name
  val category: ArchJobCategory = ArchJobCategories.None

  override val infoUrl = "https://arch-webservices.zendesk.com/hc/en-us/articles/15810489328276-ARCH-named-entities-datasets"

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

  def functions(conf: DerivationJobConf) = {
    val entities = Entities(properties(conf.params.get[String]("lang")))
    Seq(entities.of(textLoad))
  }
}

object ArchiveSparkEntityExtraction extends ArchiveSparkEntityExtraction {
  override val uuid: String = "018d114d-3426-730e-94a1-b56ca73fc1ad"
  override val name: String = "Named entities"
  override val description: String =
    "Names of persons, organizations, and geographic locations detected in each text-bearing document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."
  override val category: ArchJobCategory = ArchJobCategories.Text
}

object ArchiveSparkEntityExtractionChinese extends ArchiveSparkEntityExtraction {
  override val uuid: String = "018d1151-3a3a-7184-b6ed-8ec176ee750e"
  override val name: String = "Named entities (Chinese)"
  override val description: String =
    "Names of persons, organizations, and geographic locations detected in each text-bearing document in the collection. Output: one or more JSONL files comprising a JSON object for each input record."
  override val category: ArchJobCategory = ArchJobCategories.Text

  override def properties(lang: Option[String]): Properties = super.properties(Some("chinese"))
}

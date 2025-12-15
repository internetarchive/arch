package org.archive.webservices.ars.processing.jobs.archivespark.functions.adapters

import edu.stanford.nlp.pipeline.StanfordCoreNLP
import org.archive.webservices.archivespark.functions.EntitiesConstants
import org.archive.webservices.archivespark.model.EnrichFunc
import org.archive.webservices.archivespark.model.pointers.DataLoadPointer
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.archivespark.base.{ArchEnrichRoot, ArchiveSparkEnrichJob}
import org.archive.webservices.ars.processing.jobs.archivespark.functions.CoreNlpEntities
import org.archive.webservices.ars.util.JsonSchemaUtil.finalSchemaJson

import io.circe.Encoder
import json._
import json.schema._
import com.github.andyglow.jsonschema.AsCirce._

import java.util.Properties
import scala.collection.JavaConverters.asScalaSetConverter

object Lang extends Enumeration {
  type Lang = Value
  val English, Chinese = Value

  implicit val encodeLang: Encoder[Lang.Lang] = Encoder.encodeEnumeration(Lang)
}
import Lang._

object EntitiesAdapter extends ArchArchiveSparkFunctionAdapter[String, Seq[String]] {
  override lazy val baseFunc: EnrichFunc[_, _, String, Seq[String]] = new CoreNlpEntities()

  override def defaultDependency: Option[DataLoadPointer[ArchEnrichRoot[_], String]] = {
    Some(ArchiveSparkEnrichJob.plainTextLoad)
  }

  override def initFunc(params: DerivationJobParameters): EnrichFunc[_, _, String, Seq[String]] = {
    val langParam = params.get[String]("lang").map(_.toLowerCase)
    langParam match {
      case Some("chinese") => new CoreNlpEntities(properties(langParam), cleanLatin = false)
      case Some(_) => new CoreNlpEntities(properties(langParam))
      case None => baseFunc
    }
  }

  def properties(lang: Option[String] = None): Properties = {
    val default = EntitiesConstants.DefaultProps
    lang match {
      case Some(l) =>
        val props = new StanfordCoreNLP(l).getProperties
        for (p <- default.stringPropertyNames.asScala) {
          props.setProperty(p, default.getProperty(p))
        }
        props
      case None => default
    }
  }

  case class Params(
      @title("Language") @description("The native language of the corpus") lang: Lang =
        Lang.English)

  val paramsSchema = Some(finalSchemaJson[Params](Json.schema[Params]))
}

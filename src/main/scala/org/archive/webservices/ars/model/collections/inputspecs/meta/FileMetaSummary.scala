package org.archive.webservices.ars.model.collections.inputspecs.meta

import io.circe._
import io.circe.syntax._
import org.archive.webservices.sparkling.util.IteratorUtil

import scala.collection.immutable.ListMap

object FileMetaSummary {
  val MaxOptions = 10
  val MaxStringOptionLength = 100

  lazy val empty = new FileMetaSummary()
}

class FileMetaSummary extends Serializable {
  private var fields: ListMap[String, FileMetaFieldSummary] = ListMap.empty

  def add(meta: FileMetaData): Unit = {
    for (missing <- (fields -- meta.keys).values) missing.optional = true
    val first = fields.isEmpty
    for (field <- meta.fields) {
      fields.getOrElse(field.key, {
        val summary = new FileMetaFieldSummary
        summary.optional = !first
        fields += field.key -> summary
        summary
      }).add(field)
    }
  }

  def ++(that: FileMetaSummary): FileMetaSummary = {
    if (fields.isEmpty) that
    else if (that.fields.isEmpty) this
    else {
      val newFields = {
        fields.toSeq.map(_._1).zipWithIndex ++ that.fields.toSeq.map(_._1).zipWithIndex
      }.groupBy(_._1).toSeq.map { case (key, group) =>
        (key, group.map(_._2).min)
      }.sortBy(_._2).map(_._1).map { field =>
        val thisField = fields.get(field)
        val thatField = that.fields.get(field)
        field -> {
          if (thisField.isEmpty || thatField.isEmpty) {
            val field = thisField.orElse(thatField).get
            field.optional = true
            field
          } else thisField.get ++ thatField.get
        }
      }
      val summary = new FileMetaSummary
      summary.fields ++= newFields
      summary
    }
  }

  def toJson: Json = {
    fields.toSeq.map { case (key, field) =>
      key -> field.toJson
    }.toMap.asJson
  }

  def toJsonSchema: Json = {
    ListMap(
      "$schema" -> "https://json-schema.org/draft/2020-12/schema".asJson,
      "type" -> "object".asJson,
      "required" -> fields.toSeq.filter(!_._2.optional).map(_._1).asJson,
      "properties" -> ListMap(fields.toSeq.map { case (key, field) =>
        key -> (ListMap("title" -> key.asJson) ++ field.toJsonSchemaProperties)
      }: _*).asJson
    ).asJson
  }
}

package org.archive.webservices.ars.model.collections.inputspecs.meta

import io.circe._
import io.circe.syntax._

import scala.collection.immutable.ListMap

class FileMetaFieldSummary extends Serializable {
  var optional: Boolean = false
  var types: Map[FileMetaFieldType, FileMetaFieldTypeSummary] = Map.empty

  def add(field: FileMetaField): Unit = {
    val summary = types.getOrElse(field.fieldType, {
      val summary = field.fieldType.primitive match {
        case FileMetaFieldType.String => new FileMetaFieldStringTypeSummary
        case FileMetaFieldType.Number => new FileMetaFieldNumberTypeSummary
        case FileMetaFieldType.Boolean => FileMetaFieldBooleanTypeSummary
      }
      types += field.fieldType.primitive -> summary
      summary
    })
    if (field.fieldType.multi) {
      val values = field.value.asInstanceOf[Seq[_]]
      summary.adds(values)
    } else summary.add(field.value)
  }

  def ++(that: FileMetaFieldSummary): FileMetaFieldSummary = {
    val summary = new FileMetaFieldSummary
    summary.optional = optional || that.optional
    summary.types = (types.keySet ++ that.types.keySet).toSeq.map { t =>
      val thisType = types.get(t)
      val thatType = that.types.get(t)
      t -> (if (thisType.isEmpty || thatType.isEmpty) thisType.orElse(thatType).get else thisType.get ++ thatType.get)
    }.toMap
    summary
  }

  def toJson: Json = {
    Map(
      "optional" -> optional.asJson,
      "types" -> types.values.map(_.toJson).toMap.asJson
    ).asJson
  }

  def toJsonSchemaProperties: Seq[(String, Json)] = {
    if (types.size == 1) {
      types.head._2.toJsonSchemaProperties
    } else {
      Seq("oneOf" -> types.toSeq.map { case (_, t) =>
        ListMap(t.toJsonSchemaProperties: _*).asJson
      }.asJson)
    }
  }
}
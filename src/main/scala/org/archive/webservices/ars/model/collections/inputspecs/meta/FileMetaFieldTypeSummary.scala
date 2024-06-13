package org.archive.webservices.ars.model.collections.inputspecs.meta

import io.circe._
import io.circe.syntax._

import scala.collection.immutable.ListMap
import scala.math.Ordering.Implicits.infixOrderingOps

trait FileMetaFieldTypeSummary extends Serializable {
  def add(value: Any): Unit
  def adds(values: Seq[_]): Unit = for (v <- values) add(v)
  def ++(that: FileMetaFieldTypeSummary): FileMetaFieldTypeSummary
  def toJson: (String, Json)
  def toJsonSchemaProperties: Seq[(String, Json)]
}

abstract class FileMetaFieldOrderedTypeSummary[A : Ordering] extends FileMetaFieldTypeSummary {
  protected var empty: Boolean = true
  protected var minNumValues = 1
  protected var maxNumValues = 1
  protected var minValue: A = _
  protected var maxValue: A = _

  override def add(value: Any): Unit = {
    val v = value.asInstanceOf[A]
    if (empty || v < minValue) minValue = v
    if (empty || v > maxValue) maxValue = v
    empty = false
  }

  override def adds(values: Seq[_]): Unit = {
    super.adds(values)
    val len = values.length
    if (empty || len < minNumValues) minNumValues = len
    if (empty || len > maxNumValues) maxNumValues = len
    empty = false
  }

  def wrapJsonSchemaArray(typeSchema: Seq[(String, Json)]): Seq[(String, Json)] = {
    if (maxNumValues > 1) {
      Seq(
        "type" -> "array".asJson,
        "items" -> ListMap(typeSchema: _*).asJson,
        "minItems" -> minNumValues.asJson,
        "maxItems" -> maxNumValues.asJson,
        "uniqueItems" -> true.asJson)
    } else typeSchema
  }
}

class FileMetaFieldStringTypeSummary extends FileMetaFieldOrderedTypeSummary[String] {
  private var options = Set.empty[String]
  private var freeText = false
  private var minLength = 1
  private var maxLength = 1

  override def add(value: Any): Unit = {
    val v = value.asInstanceOf[String]

    if (!freeText && (v.length > FileMetaSummary.MaxStringOptionLength || {
      options.size == FileMetaSummary.MaxOptions && !options.contains(v)
    })) freeText = true

    if (options.size < FileMetaSummary.MaxOptions) {
      if (v.length <= FileMetaSummary.MaxStringOptionLength) options += v
      else options += (v.take(FileMetaSummary.MaxStringOptionLength) + "...")
    }

    val len = v.length
    if (empty || len < minLength) minLength = len
    if (empty || len > maxLength) maxLength = len

    super.add(value)
  }

  override def ++(that: FileMetaFieldTypeSummary): FileMetaFieldTypeSummary = {
    val thatTyped = that.asInstanceOf[FileMetaFieldStringTypeSummary]
    if (empty) thatTyped
    else if (thatTyped.empty) this
    else {
      val summary = new FileMetaFieldStringTypeSummary
      summary.empty = false
      summary.minNumValues = minNumValues.min(thatTyped.minNumValues)
      summary.maxNumValues = maxNumValues.max(thatTyped.maxNumValues)
      summary.minValue = if (minValue < thatTyped.minValue) minValue else thatTyped.minValue
      summary.maxValue = if (maxValue > thatTyped.maxValue) maxValue else thatTyped.maxValue
      summary.minLength = minLength.min(thatTyped.minLength)
      summary.maxLength = maxLength.max(thatTyped.maxLength)
      val newOptions = options ++ thatTyped.options
      summary.freeText = freeText || thatTyped.freeText || newOptions.size > FileMetaSummary.MaxOptions
      summary.options = newOptions.take(FileMetaSummary.MaxOptions)
      summary
    }
  }

  override def toJson: (String, Json) = "string" -> {
    val multiValues = maxNumValues > 1
    ListMap("multiValues" -> multiValues.asJson) ++ {
      if (multiValues) Seq(
        "minNumValues" -> minNumValues.asJson,
        "maxNumValues" -> maxNumValues.asJson
      ) else Seq.empty
    } ++ Seq(
      "freeText" -> freeText.asJson
    ) ++ {
      if (freeText) Seq(
        "minLength" -> minLength.asJson,
        "maxLength" -> maxLength.asJson,
        "examples" -> options.toSeq.sorted.asJson
      ) else Seq(
        "options" -> options.toSeq.sorted.asJson
      )
    }
  }.asJson

  override def toJsonSchemaProperties: Seq[(String, Json)] = {
    val typeSchema = Seq("type" -> "string".asJson) ++ {
      if (freeText) Seq(
        "minLength" -> minLength.asJson,
        "maxLength" -> maxLength.asJson,
        "examples" -> options.toSeq.sorted.asJson
      ) else Seq(
        "enum" -> options.toSeq.sorted.asJson
      )
    }
    wrapJsonSchemaArray(typeSchema)
  }
}

class FileMetaFieldNumberTypeSummary extends FileMetaFieldOrderedTypeSummary[Double] {
  private var options = Set.empty[Double]
  private var isDecimal = false
  private var freeRange = false

  override def add(value: Any): Unit = {
    val v = value.asInstanceOf[Double]
    if (v % 1 != 0) isDecimal = true
    if (!freeRange && options.size < FileMetaSummary.MaxOptions) options += v
    else if (!options.contains(v)) freeRange = true
    super.add(value)
  }

  override def ++(that: FileMetaFieldTypeSummary): FileMetaFieldTypeSummary = {
    val thatTyped = that.asInstanceOf[FileMetaFieldNumberTypeSummary]
    if (empty) thatTyped
    else if (thatTyped.empty) this
    else {
      val summary = new FileMetaFieldNumberTypeSummary
      summary.empty = false
      summary.minNumValues = minNumValues.min(thatTyped.minNumValues)
      summary.maxNumValues = maxNumValues.max(thatTyped.maxNumValues)
      summary.minValue = minValue.min(thatTyped.minValue)
      summary.maxValue = maxValue.max(thatTyped.maxValue)
      summary.isDecimal = isDecimal || thatTyped.isDecimal
      summary.freeRange = freeRange || thatTyped.freeRange
      if (!summary.freeRange) {
        val newOptions = options ++ thatTyped.options
        if (options.size <= FileMetaSummary.MaxOptions) summary.options = newOptions
        else summary.freeRange = true
      }
      summary
    }
  }

  override def toJson: (String, Json) = "number" -> {
    val multiValues = maxNumValues > 1
    ListMap("multiValues" -> multiValues.asJson) ++ {
      if (multiValues) Seq(
        "minNumValues" -> minNumValues.asJson,
        "maxNumValues" -> maxNumValues.asJson
      ) else Seq.empty
    } ++ Seq(
      "freeRange" -> freeRange.asJson,
      "isDecimal" -> isDecimal.asJson
    ) ++ {
      if (isDecimal) {
        if (freeRange) Seq(
          "minValue" -> minValue.asJson,
          "maxValue" -> maxValue.asJson
        ) else Seq(
          "options" -> options.toSeq.sorted.asJson
        )
      } else {
        if (freeRange) Seq(
          "minValue" -> minValue.toLong.asJson,
          "maxValue" -> maxValue.toLong.asJson
        ) else Seq(
          "options" -> options.map(_.toLong).toSeq.sorted.asJson
        )
      }
    }
  }.asJson

  override def toJsonSchemaProperties: Seq[(String, Json)] = {
    val typeSchema = {
      if (isDecimal) {
        Seq("type" -> "number".asJson) ++ {
          if (freeRange) {
            Seq("minimum" -> minValue.asJson, "maximum" -> maxValue.asJson)
          } else {
            Seq("enum" -> options.toSeq.sorted.asJson)
          }
        }
      } else {
        Seq("type" -> "integer".asJson) ++ {
          if (freeRange) {
            Seq("minimum" -> minValue.toLong.asJson, "maximum" -> maxValue.toLong.asJson)
          } else {
            Seq("enum" -> options.map(_.toLong).toSeq.sorted.asJson)
          }
        }
      }
    }
    wrapJsonSchemaArray(typeSchema)
  }
}

object FileMetaFieldBooleanTypeSummary extends FileMetaFieldTypeSummary {
  override def add(value: Any): Unit = {}

  override def toJson: (String, Json) = "boolean" -> Map.empty[String, Json].asJson

  override def ++(that: FileMetaFieldTypeSummary): FileMetaFieldTypeSummary = this

  override def toJsonSchemaProperties: Seq[(String, Json)] = Seq("type" -> "boolean".asJson)
}
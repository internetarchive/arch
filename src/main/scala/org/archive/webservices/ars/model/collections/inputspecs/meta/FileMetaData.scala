package org.archive.webservices.ars.model.collections.inputspecs.meta

import io.circe.Json
import io.circe.syntax.EncoderOps
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.archive.webservices.sparkling.cdx.CdxRecord

import scala.collection.immutable.ListMap

class FileMetaData(val fields: Seq[FileMetaField]) extends Serializable {
  lazy val map: Map[String, FileMetaField] = ListMap(fields.map(f => f.key -> f): _*)
  def str(key: String): Option[String] =
    map.get(key).flatMap(_.get[String]).orElse(strs(key).headOption)
  def strs(key: String): Seq[String] = map.get(key).toSeq.flatMap(_.gets[String])
  def int(key: String): Option[Int] =
    map.get(key).flatMap(_.get[Int]).orElse(ints(key).headOption)
  def ints(key: String): Seq[Int] = map.get(key).toSeq.flatMap(_.gets[Int])
  def keys: Set[String] = map.keySet
  def toJson: Json = map.mapValues(_.toJson).asJson
}

object FileMetaData {
  lazy val empty = new FileMetaData(Seq.empty)

  def fromCdx(cdx: CdxRecord): FileMetaData = FileMetaData(
    FileMetaField("surtUrl", cdx.surtUrl),
    FileMetaField("timestamp", cdx.timestamp),
    FileMetaField("originalUrl", cdx.originalUrl),
    FileMetaField("mime", cdx.mime),
    FileMetaField("status", cdx.status),
    FileMetaField("digest", cdx.digest),
    FileMetaField("redirectUrl", cdx.redirectUrl),
    FileMetaField("meta", cdx.meta))

  def fromParquet(structType: StructType, row: Row): FileMetaData = {
    val fields = structType.fields.toSeq.flatMap { field =>
      Option(row.get(row.fieldIndex(field.name))).flatMap { value =>
        {
          field.dataType match {
            case StringType => Some(FileMetaFieldType.String)
            case IntegerType => Some(FileMetaFieldType.Number)
            case LongType => Some(FileMetaFieldType.Number)
            case DoubleType => Some(FileMetaFieldType.Number)
            case BooleanType => Some(FileMetaFieldType.Boolean)
            case array: ArrayType =>
              array.elementType match {
                case StringType => Some(FileMetaFieldType.Strings)
                case IntegerType => Some(FileMetaFieldType.Numbers)
                case LongType => Some(FileMetaFieldType.Numbers)
                case DoubleType => Some(FileMetaFieldType.Numbers)
                case BooleanType => Some(FileMetaFieldType.Booleans)
                case _ => None
              }
            case _ => None
          }
        }.map {
          FileMetaField(field.name, value, _)
        }
      }
    }
    new FileMetaData(fields)
  }

  def fromJson(json: Json): FileMetaData = {
    val cursor = json.hcursor
    val fields = cursor.keys.toSeq.flatten
      .flatMap { key =>
        cursor.downField(key).focus.map(key -> _)
      }
      .filter(!_._2.isNull)
      .flatMap { case (k, v) =>
        {
          if (v.isString) v.asString.map(_ -> FileMetaFieldType.String)
          else if (v.isNumber) v.asNumber.map(_.toDouble -> FileMetaFieldType.Number)
          else if (v.isBoolean) v.asBoolean.map(_ -> FileMetaFieldType.Boolean)
          else if (v.isArray) v.asArray.flatMap { array =>
            array.headOption.flatMap { head =>
              if (head.isString) Some(array.flatMap(_.asString) -> FileMetaFieldType.Strings)
              else if (head.isNumber)
                Some(array.flatMap(_.asNumber.map(_.toDouble)) -> FileMetaFieldType.Numbers)
              else if (head.isBoolean)
                Some(array.flatMap(_.asBoolean) -> FileMetaFieldType.Booleans)
              else None
            }
          }
          else None
        }.map { case (value, t) =>
          FileMetaField(k, value, t)
        }
      }
    new FileMetaData(fields)
  }

  def stringValues(map: Map[String, String]): FileMetaData = {
    val fields = map.toSeq.map { case (k, v) =>
      FileMetaField(k, v, FileMetaFieldType.String)
    }
    new FileMetaData(fields)
  }

  def apply(fields: FileMetaField*): FileMetaData = new FileMetaData(fields)
}

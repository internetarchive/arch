package org.archive.webservices.ars.model.collections.inputspecs.meta

import _root_.io.circe.syntax._
import io.circe.Json

trait FileMetaFieldType extends Serializable {
  def primitive: FileMetaFieldType = this
  def multi: Boolean = false
  def toJson(value: Any): Json
}

trait FileMetaFieldMultiType extends FileMetaFieldType {
  def primitive: FileMetaFieldType
  override def multi: Boolean = true
  def toJson(value: Any): Json = value.asInstanceOf[Seq[_]].map(primitive.toJson).asJson
}

object FileMetaFieldType {
  case object String extends FileMetaFieldType {
    override def toJson(value: Any): Json = value.asInstanceOf[String].asJson
  }

  case object Number extends FileMetaFieldType {
    override def toJson(value: Any): Json = {
      value match {
        case i: Int => i.asJson
        case l: Long => l.asJson
        case d: Double => d.asJson
      }
    }
  }

  case object Boolean extends FileMetaFieldType {
    override def toJson(value: Any): Json = value.asInstanceOf[Boolean].asJson
  }

  case object Strings extends FileMetaFieldMultiType {
    override def primitive: FileMetaFieldType = String
  }

  case object Numbers extends FileMetaFieldMultiType {
    override def primitive: FileMetaFieldType = Number
  }

  case object Booleans extends FileMetaFieldMultiType {
    override def primitive: FileMetaFieldType = Boolean
  }
}

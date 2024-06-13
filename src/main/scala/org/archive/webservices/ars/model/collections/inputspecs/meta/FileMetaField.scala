package org.archive.webservices.ars.model.collections.inputspecs.meta

import io.circe.Json

import scala.reflect.ClassTag

class FileMetaField private (val key: String, val value: Any, val fieldType: FileMetaFieldType)
    extends Serializable {
  def get[A: ClassTag]: Option[A] = implicitly[ClassTag[A]].unapply(value)
  def gets[A: ClassTag]: Seq[A] =
    get[Seq[_]].toSeq.flatMap(_.flatMap(implicitly[ClassTag[A]].unapply))
  def toJson: Json = fieldType.toJson(value)
}

object FileMetaField {
  def apply(key: String, value: Any, fieldType: FileMetaFieldType): FileMetaField = {
    new FileMetaField(key, value, fieldType)
  }

  def apply(key: String, value: String): FileMetaField =
    FileMetaField(key, value, FileMetaFieldType.String)
  def apply(key: String, value: Int): FileMetaField =
    FileMetaField(key, value, FileMetaFieldType.Number)
  def apply(key: String, value: Long): FileMetaField =
    FileMetaField(key, value, FileMetaFieldType.Number)
  def apply(key: String, value: Boolean): FileMetaField =
    FileMetaField(key, value, FileMetaFieldType.Boolean)
}

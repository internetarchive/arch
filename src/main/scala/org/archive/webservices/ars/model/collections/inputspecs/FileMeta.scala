package org.archive.webservices.ars.model.collections.inputspecs

import io.circe.HCursor

import scala.reflect.{ClassTag, classTag}
import scala.util.Try

trait FileMeta extends Serializable {
  def str(key: String): Option[String] = get[String](key).orElse(strs(key).headOption)
  def strs(key: String): Seq[String] = gets[String](key)
  def int(key: String): Option[Int] = get[Int](key)
  def get[A: ClassTag](key: String): Option[A]
  def gets[A: ClassTag](key: String): Seq[A]
}

object FileMeta {
  lazy val empty = new FileMetaMap(Map.empty)
  def apply(map: Map[String, Any]): FileMeta = new FileMetaMap(map)
  def apply(cursor: HCursor): FileMeta = new FileMetaJson(cursor)
}

class FileMetaMap(map: Map[String, Any]) extends FileMeta {
  override def get[A: ClassTag](key: String): Option[A] =
    map.get(key).flatMap(v => Try(v.asInstanceOf[A]).toOption)
  override def gets[A: ClassTag](key: String): Seq[A] = get[Array[A]](key).toSeq.flatten
}

class FileMetaJson(cursor: HCursor) extends FileMeta {
  override def get[A: ClassTag](key: String): Option[A] = {
    classTag[A] match {
      case t if t == classTag[String] => cursor.get[String](key).toOption
      case t if t == classTag[Int] => cursor.get[Int](key).toOption
      case t if t == classTag[Long] => cursor.get[Long](key).toOption
      case t => throw new UnsupportedOperationException(t.toString)
    }
  }.map(_.asInstanceOf[A])

  override def gets[A: ClassTag](key: String): Seq[A] = {
    classTag[A] match {
      case t if t == classTag[String] => cursor.get[Array[String]](key).toOption
      case t if t == classTag[Int] => cursor.get[Array[Int]](key).toOption
      case t if t == classTag[Long] => cursor.get[Array[Long]](key).toOption
      case t => throw new UnsupportedOperationException(t.toString)
    }
  }.map(_.map(_.asInstanceOf[A])).toSeq.flatten
}

package org.archive.webservices.ars.model

import io.circe._
import io.circe.syntax._

import java.lang.reflect.{Field, ParameterizedType, Type}
import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

package object api {
  trait ApiResponseObject[T <: ApiResponseObject[T]] extends Product {
    def get[A](field: String)(implicit responseType: ApiResponseType[T]): Option[A] = {
      responseType._fields
        .get(field)
        .map(_.get(this))
        .flatMap {
          case opt: Option[_] => opt
          case v => Some(v)
        }
        .map(_.asInstanceOf[A])
    }

    def toJson(implicit responseType: ApiResponseType[T]): Json =
      ListMap(responseType.fields.toSeq.map { case (field, fieldType) =>
        field -> (fieldType match {
          case ApiFieldType.Boolean => get[Boolean](field).asJson
          case ApiFieldType.Int => get[Int](field).asJson
          case ApiFieldType.Integer => get[Integer](field).asJson
          case ApiFieldType.Long => get[Long](field).asJson
          case ApiFieldType.String => get[String](field).asJson
          case ApiFieldType.Json => get[Json](field).asJson
          case ApiFieldType.Seq =>
            field match {
              case "jobs" => get[Seq[AvailableJob]](field).get.map(_.toJson).asJson
              case "files" => get[Seq[WasapiResponseFile]](field).get.map(_.toJson).asJson
              case "locations" => get[Seq[String]](field).get.map(_.asJson).asJson
            }
          case ApiFieldType.Map =>
            field match {
              case "checksums" => get[Map[String, String]](field).get.asJson
            }
        })
      }: _*).asJson
  }

  object ApiFieldType extends Enumeration {
    val Boolean, Int, Integer, Long, String, Json, Map, Seq = Value
  }

  private val TypeMap: Map[Type, ApiFieldType.Value] = Map(
    classOf[Boolean] -> ApiFieldType.Boolean,
    classOf[java.lang.Boolean] -> ApiFieldType.Boolean,
    classOf[Int] -> ApiFieldType.Int,
    classOf[Integer] -> ApiFieldType.Integer,
    classOf[Long] -> ApiFieldType.Long,
    classOf[String] -> ApiFieldType.String,
    classOf[Json] -> ApiFieldType.Json,
    // classOf[Seq[A]] yields same as classOf[Seq[B]] - same for Map
    classOf[Seq[_]] -> ApiFieldType.Seq,
    classOf[Map[_, _]] -> ApiFieldType.Map)

  class ApiResponseType[T <: ApiResponseObject[T]](classTag: ClassTag[T]) {
    private implicit val self: ApiResponseType[T] = this

    private[api] val _fields: Map[String, Field] = ListMap(
      classTag.runtimeClass.getDeclaredFields.map { f =>
        f.setAccessible(true)
        (f.getName, f)
      }: _*)

    val fields: Map[String, ApiFieldType.Value] = ListMap(
      _fields.toSeq.map { case (name, field) =>
        val fieldType = field.getType
        val valueType =
          if (fieldType == classOf[Option[_]])
            field.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments.head
          else fieldType
        name -> TypeMap(valueType)
      }: _*)

    def ordering(field: String, reverse: Boolean = false): Ordering[T] = {
      val ordering: Ordering[T] = fields.getOrElse(field, fields.head._2) match {
        case ApiFieldType.Boolean => Ordering.by(_.get[Boolean](field))
        case ApiFieldType.Int => Ordering.by(_.get[Int](field))
        case ApiFieldType.Integer => Ordering.by(_.get[Integer](field))
        case ApiFieldType.Long => Ordering.by(_.get[Long](field))
        case ApiFieldType.String => Ordering.by(_.get[String](field))
        case ApiFieldType.Json => Ordering.by(_.get[String](field))
      }
      if (reverse) ordering.reverse else ordering
    }
  }

  implicit def apiResponseType[T <: ApiResponseObject[T]: ClassTag]: ApiResponseType[T] =
    new ApiResponseType[T](implicitly[ClassTag[T]])
}

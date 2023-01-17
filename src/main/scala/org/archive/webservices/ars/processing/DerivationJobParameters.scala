package org.archive.webservices.ars.processing

import io.circe.{Decoder, Encoder, Json}
import io.circe.parser._
import io.circe.syntax._

case class DerivationJobParameters (values: Map[String, Json]) extends Serializable {
  def size: Int = values.size
  def isEmpty: Boolean = size == 0
  def nonEmpty: Boolean = !isEmpty

  def set[A : Encoder](key: String, value: A): DerivationJobParameters = {
    DerivationJobParameters(values.updated(key, value.asJson))
  }

  def set[A: Encoder](keyValues: (String, A)*): DerivationJobParameters = {
    DerivationJobParameters(values ++ keyValues.map{case (k,v) => k -> v.asJson})
  }

  def set(keyValues: (String, Json)*): DerivationJobParameters = {
    DerivationJobParameters(values ++ keyValues)
  }

  def get[A : Decoder](key: String): Option[A] = values.get(key).flatMap(_.as[A].toOption)

  def toJson: Json = values.asJson
}

object DerivationJobParameters {
  val Empty: DerivationJobParameters = DerivationJobParameters(Map.empty)

  def fromJson(json: Json): Option[DerivationJobParameters] = {
    val cursor = json.hcursor
    cursor.keys.map { keys =>
      val params = keys.flatMap { key =>
        cursor.downField(key).focus.map(key -> _)
      }.toMap
      DerivationJobParameters(params)
    }
  }

  def fromJson(json: String): Option[DerivationJobParameters] = parse(json).right.toOption.flatMap(fromJson)
}
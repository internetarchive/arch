package org.archive.webservices.ars.model.collections.inputspecs

import io.circe.HCursor
import io.circe.parser.parse
import io.circe.syntax._
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.collections.FileCollectionSpecifics

import scala.util.Try

trait InputSpec {
  def id: String
  def specType: String
  def inputType: String
  def cursor: HCursor
  def size: Long
  def str(key: String): Option[String] = cursor.get[String](key).toOption
  def int(key: String): Option[Int] = cursor.get[Int](key).toOption
  lazy val loader: InputSpecLoader = InputSpecLoader.get(this).getOrElse {
    throw new UnsupportedOperationException("no loader found for input spec type " + specType)
  }
}

class DefaultInputSpec(val specType: String, val cursor: HCursor) extends InputSpec {
  override lazy val id: String =
    cursor.get[String]("id").getOrElse(specType + ":" + cursor.focus.get.noSpaces.hashCode)
  override lazy val inputType: String =
    cursor.get[String]("inputType").getOrElse(InputSpec.InputType.Files)
  override lazy val size: Long = cursor.get[Long]("size").getOrElse(-1)
}

class CollectionBasedInputSpec(
    val collectionId: String,
    val inputPath: String,
    specOpt: Option[HCursor] = None)
    extends InputSpec {
  override val id: String = collectionId
  override val specType: String = CollectionBasedInputSpec.SpecType
  override val inputType: String = InputSpec.InputType.WARC
  override lazy val cursor: HCursor = specOpt.getOrElse(
    Map(
      "type" -> specType,
      "collectionId" -> collectionId,
      "inputPath" -> inputPath).asJson.hcursor)
  override def size: Long = ArchCollection.get(collectionId).map(_.stats.size).getOrElse(-1)
  private[inputspecs] var _collection: Option[ArchCollection] = None
  def collection: ArchCollection = _collection
    .orElse {
      _collection = ArchCollection.get(collectionId)
      _collection
    }
    .getOrElse {
      throw new RuntimeException(s"Collection ${collectionId} not found.")
    }
}

object CollectionBasedInputSpec {
  val SpecType = "collection"
}

object InputSpec {
  object InputType {
    val Files = "files"
    val WARC = "warc"
  }

  case class Identifier private (str: String) {
    override def toString: String = str
  }

  implicit def toIdentifier(spec: InputSpec): Identifier = Identifier(spec.id)

  def apply(spec: String): InputSpec = apply(parse(spec).right.toOption.map(_.hcursor).getOrElse {
    throw new RuntimeException("invalid input spec")
  })

  def apply(cursor: HCursor): InputSpec = {
    val specType = cursor.get[String]("type").toOption.orElse {
      throw new RuntimeException("invalid input spec: type missing")
    }
    specType
      .filter(_ == CollectionBasedInputSpec.SpecType)
      .flatMap { _ =>
        for {
          collectionId <- cursor.get[String]("collectionId").toOption
          inputPath <- cursor.get[String]("inputPath").toOption
        } yield {
          new CollectionBasedInputSpec(collectionId, inputPath, Some(cursor))
        }
      }
      .getOrElse {
        new DefaultInputSpec(specType.get, cursor)
      }
  }

  def apply(collection: ArchCollection, inputPath: String): InputSpec = {
    val spec = apply(collection.id, inputPath)
    if (isCollectionBased(spec)) spec._collection = Some(collection)
    spec
  }

  def apply(collectionId: String, inputPath: String): InputSpec = {
    if (collectionId.startsWith(FileCollectionSpecifics.Prefix)) apply(inputPath)
    else new CollectionBasedInputSpec(collectionId, inputPath)
  }

  def isCollectionBased(spec: InputSpec): Boolean = spec.isInstanceOf[CollectionBasedInputSpec]

  implicit def toCollectionBased(spec: InputSpec): CollectionBasedInputSpec =
    Try(spec.asInstanceOf[CollectionBasedInputSpec]).getOrElse {
      throw new UnsupportedOperationException("this spec is not collection-based.")
    }
}

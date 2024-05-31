package org.archive.webservices.ars.model.collections.inputspecs

import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Decoder, HCursor, Json}
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.collections.FileCollectionSpecifics
import org.archive.webservices.ars.processing.{DerivationJobInstance, DerivationJobParameters, JobManager, ProcessingState}

import scala.util.Try

trait InputSpec {
  def id: String
  def specType: String
  def inputType: String
  def cursor: HCursor
  def downField(key: String): Option[Json] = cursor.downField(key).focus
  def params(key: String): Option[DerivationJobParameters] = downField(key).flatMap(DerivationJobParameters.fromJson)
  def str(key: String): Option[String] = get[String](key)
  def int(key: String): Option[Int] = get[Int](key)
  def get[A: Decoder](key: String): Option[A] = cursor.get[A](key).toOption
  lazy val size: Long = loader.size(this)
  lazy val loader: InputSpecLoader = InputSpecLoader.get(this).getOrElse {
    throw new UnsupportedOperationException("no loader found for input spec type " + specType)
  }
}

class DefaultInputSpec(val specType: String, val cursor: HCursor, idOpt: Option[String] = None)
    extends InputSpec {
  override lazy val id: String = idOpt.getOrElse {
    cursor.get[String]("id").getOrElse(specType + ":" + cursor.focus.get.noSpaces.hashCode)
  }
  override lazy val inputType: String =
    cursor.get[String]("inputType").getOrElse(loader.inputType(this).getOrElse(InputSpec.InputType.Files))
}

class CollectionBasedInputSpec(
    val collectionId: String,
    inputPathOpt: Option[String] = None,
    cursorOpt: Option[HCursor] = None)
    extends InputSpec {
  override val id: String = collectionId
  override val specType: String = ArchCollectionSpecLoader.specType
  override val inputType: String = InputSpec.InputType.WARC
  lazy val inputPath: String = inputPathOpt.getOrElse(collection.specifics.inputPath)
  override lazy val cursor: HCursor = cursorOpt.getOrElse(
    Map(
      "type" -> specType,
      "collectionId" -> collectionId,
      "inputPath" -> inputPath).asJson.hcursor)
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

class DatasetBasedInputSpec(val uuid: String, cursorOpt: Option[HCursor] = None)
    extends InputSpec {
  @transient lazy val dataset: DerivationJobInstance =
    JobManager.getInstance(uuid).filter(_.state == ProcessingState.Finished).getOrElse {
      throw new RuntimeException("Dataset with UUID " + uuid + " not found.")
    }
  override lazy val inputType: String =
    cursor.get[String]("inputType").getOrElse(InputSpec.InputType.Files)
  override val id: String = {
    val jobId = dataset.job.id + (if (dataset.conf.isSample) "-sample" else "")
    dataset.conf.inputSpec.id + "_" + jobId + "_" + uuid
  }
  override val specType: String = DatasetSpecLoader.specType
  override lazy val cursor: HCursor = cursorOpt.getOrElse(Map("uuid" -> uuid).asJson.hcursor)
  def toFileSpec: Option[InputSpec] = {
    dataset.job.datasetGlobMime(dataset.conf).map { case (glob, mime) =>
      new DefaultInputSpec(
        FileSpecLoader.specType,
        Map(
          InputSpec.DataLocationKey -> glob.asJson,
          FileSpecLoader.MimeKey -> mime.asJson).asJson.hcursor,
        Some(id))
    }
  }
}

object InputSpec {
  val DataSourceKey = "data-source"
  val MetaSourceKey = "meta-source"
  val DataLocationKey = "data-location"
  val MetaLocationKey = "meta-location"

  object InputType {
    val Files = "files"
    val WARC = "warc"
    val CDX = "cdx"

    def warc(inputType: String): Boolean = inputType == WARC || inputType == CDX
  }

  case class Identifier private (str: String) {
    override def toString: String = str
  }

  implicit def toIdentifier(spec: InputSpec): Identifier = Identifier(spec.id)

  def apply(spec: String): InputSpec = apply(spec, None)

  def apply(spec: String, id: Option[String]): InputSpec = apply(
    parse(spec).right.toOption.map(_.hcursor).getOrElse {
      throw new RuntimeException("invalid input spec")
    },
    id)

  def apply(cursor: HCursor, id: Option[String] = None): InputSpec = {
    val specType = cursor.get[String]("type").toOption.getOrElse {
      throw new RuntimeException("invalid input spec: type missing")
    }
    specType match {
      case ArchCollectionSpecLoader.specType =>
        cursor
          .get[String]("collectionId")
          .toOption
          .map { collectionId =>
            val inputPath = cursor.get[String]("inputPath").toOption
            new CollectionBasedInputSpec(collectionId, inputPath, Some(cursor))
          }
          .getOrElse {
            throw new RuntimeException("invalid input spec: collectionId missing")
          }
      case DatasetSpecLoader.specType =>
        cursor
          .get[String]("uuid")
          .toOption
          .map { uuid =>
            new DatasetBasedInputSpec(uuid, Some(cursor))
          }
          .getOrElse {
            throw new RuntimeException("invalid input spec: uuid missing")
          }
      case _ =>
        new DefaultInputSpec(specType, cursor, id)
    }
  }

  def apply(collection: ArchCollection, inputPath: String): InputSpec = {
    val spec = apply(collection.id, inputPath)
    if (isCollectionBased(spec)) spec._collection = Some(collection)
    spec
  }

  def apply(collectionId: String, inputPath: String): InputSpec = {
    if (collectionId.startsWith(FileCollectionSpecifics.Prefix))
      apply(inputPath, Some(collectionId))
    else new CollectionBasedInputSpec(collectionId, Some(inputPath))
  }

  def isCollectionBased(spec: InputSpec): Boolean = spec.isInstanceOf[CollectionBasedInputSpec]

  def isDatasetBased(spec: InputSpec): Boolean = spec.isInstanceOf[DatasetBasedInputSpec]

  implicit def toCollectionBased(spec: InputSpec): CollectionBasedInputSpec =
    Try(spec.asInstanceOf[CollectionBasedInputSpec]).getOrElse {
      throw new UnsupportedOperationException("this spec is not collection-based.")
    }

  implicit def toDatasetBased(spec: InputSpec): DatasetBasedInputSpec =
    Try(spec.asInstanceOf[DatasetBasedInputSpec]).getOrElse {
      throw new UnsupportedOperationException("this spec is not dataset-based.")
    }
}

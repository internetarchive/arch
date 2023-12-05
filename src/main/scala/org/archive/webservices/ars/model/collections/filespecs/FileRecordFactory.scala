package org.archive.webservices.ars.model.collections.filespecs

import io.circe.HCursor
import org.archive.webservices.ars.io.CollectionAccessContext

import java.io.InputStream

trait FileRecordFactory[Meta] extends Serializable {
  @transient var accessContext: CollectionAccessContext = CollectionAccessContext.fromLocalArchConf
  def get(filename: String, mime: String, meta: Meta): FileRecord
  def accessFile(filename: String, resolve: Boolean = true, accessContext: CollectionAccessContext = accessContext): InputStream
}

object FileRecordFactory {
  def apply[Meta](spec: HCursor): FileRecordFactory[Meta] = spec.get[String]("remote-source").toOption.flatMap {
    case "s3" => Some(S3FileRecordFactory(spec))
    case _ => None
  }.map(_.asInstanceOf[FileRecordFactory[Meta]]).getOrElse {
    throw new UnsupportedOperationException()
  }
}
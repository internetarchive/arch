package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.{CollectionAccessContext, IOHelper}

import java.io.InputStream

trait FileRecordFactory extends Serializable {
  @transient var accessContext: CollectionAccessContext =
    CollectionAccessContext.fromLocalArchConf
  def get(filename: String, mime: String, meta: FileMeta): FileRecord
  def accessFile(
      filePath: String,
      resolve: Boolean = true,
      accessContext: CollectionAccessContext = accessContext): InputStream
}

object FileRecordFactory {
  def apply(spec: InputSpec): FileRecordFactory = spec
    .str("data-source")
    .flatMap {
      case "s3" => Some(S3FileRecordFactory(spec))
      case "s3-http" => Some(S3HttpFileRecordFactory(spec))
      case "http" => Some(HttpFileRecordFactory(spec))
      case "hdfs" => Some(HdfsFileRecordFactory(spec))
      case _ => None
    }
    .getOrElse {
      throw new UnsupportedOperationException()
    }

  def filePath(path: String, filename: String): String = IOHelper.concatPaths(path, filename)
}

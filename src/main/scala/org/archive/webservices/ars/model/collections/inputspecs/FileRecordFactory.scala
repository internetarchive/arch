package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.{FileAccessContext, IOHelper}
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaData

import java.io.InputStream

trait FileRecordFactory extends Serializable {
  def companion: FileFactoryCompanion
  def dataSourceType: String = companion.dataSourceType
  @transient var accessContext: FileAccessContext =
    FileAccessContext.fromLocalArchConf
  def get(file: String, mime: String, meta: FileMetaData): FileRecord
  def accessFile(
      file: String,
      resolve: Boolean = true,
      accessContext: FileAccessContext = accessContext): InputStream
}

trait FileFactoryCompanion {
  def dataSourceType: String
  def apply(spec: InputSpec): FileRecordFactory
}

object FileRecordFactory {
  val factories: Seq[FileFactoryCompanion] = Seq(
    S3FileRecordFactory,
    S3HttpFileRecordFactory,
    HttpFileRecordFactory,
    HdfsFileRecordFactory,
    VaultFileRecordFactory,
    PetaboxFileRecordFactory)

  def apply(spec: InputSpec, default: FileFactoryCompanion): FileRecordFactory = {
    apply(spec, Some(default))
  }

  def apply(spec: InputSpec, default: Option[FileFactoryCompanion] = None): FileRecordFactory = {
    spec
      .str(InputSpec.DataSourceKey)
      .flatMap { dataSource =>
        factories.find { factory =>
          factory.dataSourceType == dataSource
        }
      }
      .orElse(default)
      .getOrElse {
        throw new UnsupportedOperationException()
      }
      .apply(spec)
  }

  def filePath(path: String, filename: String): String = IOHelper.concatPaths(path, filename)
}

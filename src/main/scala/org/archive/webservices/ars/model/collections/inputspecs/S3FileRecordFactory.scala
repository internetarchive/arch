package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.{FileAccessContext, FileAccessKeyRing, FilePointer, IOHelper}
import org.archive.webservices.sparkling.io.{CleanupInputStream, IOUtil, S3Client}

import java.io.{BufferedInputStream, FileInputStream, InputStream}

class S3FileRecordFactory(
    location: String,
    endpoint: String,
    accessKey: String,
    secretKey: String,
    bucket: String,
    longestPrefixMapping: Boolean)
    extends FileRecordFactory
    with LongestPrefixProbing {
  def companion = S3FileRecordFactory

  class S3FileRecord private[S3FileRecordFactory] (
      file: String,
      val mime: String,
      val meta: FileMeta)
      extends FileRecord {
    override lazy val filePath: String = locateFile(file)
    override def access: InputStream = accessFile(filePath, resolve = false)
    override def pointer: FilePointer = FilePointer(IOHelper.concatPaths(endpoint, bucket, filePath), filename)
  }

  override def get(file: String, mime: String, meta: FileMeta): FileRecord =
    new S3FileRecord(file, mime, meta)

  private def s3[R](action: S3Client => R): R = {
    S3Client(endpoint, accessKey, secretKey).access(action)
  }

  def accessFile(
      file: String,
      resolve: Boolean = true,
      accessContext: FileAccessContext): InputStream = {
    val path =
      if (resolve) locateFile(file) else file
    println(s"Reading $path...")
    val tmpFile = IOUtil.tmpFile
    try {
      s3(_.transfers.download(bucket, path, tmpFile).waitForCompletion())
    } catch {
      case _: Exception => tmpFile.delete()
    }
    val in = new BufferedInputStream(new FileInputStream(tmpFile))
    new CleanupInputStream(in, tmpFile.delete)
  }

  def locateFile(file: String): String = FileRecordFactory.filePath({
    if (longestPrefixMapping) IOHelper.concatPaths(location, locateLongestPrefixPath(file))
    else location
  }, file)

  private val prefixes = collection.mutable.Map.empty[String, Set[String]]
  protected def nextPrefixes(prefix: String): Set[String] = {
    prefixes.getOrElseUpdate(
      prefix, {
        s3(_.list(bucket, IOHelper.concatPaths(location, prefix))).map { key =>
          key.stripPrefix(location).stripPrefix("/")
        }
      })
  }
}

object S3FileRecordFactory extends FileFactoryCompanion {
  val dataSourceType: String = "s3"

  def apply(spec: InputSpec): S3FileRecordFactory = {
    for {
      endpoint <- spec.str("s3-endpoint")
      (accessKey, secretKey) <- FileAccessKeyRing.forUrl(endpoint).flatMap {
        case (FileAccessKeyRing.AccessMethodS3, Array(accessKey, secretKey)) =>
          Some((accessKey, secretKey))
        case _ => None
      }.orElse {
        for {
          accessKey <- spec.str("s3-accessKey")
          secretKey <- spec.str("s3-secretKey")
        } yield (accessKey, secretKey)
      }
      bucket <- spec.str("s3-bucket")
      location <- spec.str(InputSpec.DataLocationKey)
    } yield {
      val longestPrefixMapping = spec.str("data-path-mapping").contains("longest-prefix")
      new S3FileRecordFactory(
        location,
        endpoint,
        accessKey,
        secretKey,
        bucket,
        longestPrefixMapping)
    }
  }.getOrElse {
    throw new RuntimeException("No location URL specified.")
  }
}

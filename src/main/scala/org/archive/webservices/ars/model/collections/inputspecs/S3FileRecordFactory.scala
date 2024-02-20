package org.archive.webservices.ars.model.collections.inputspecs

import org.archive.webservices.ars.io.CollectionAccessContext
import org.archive.webservices.sparkling.io.{CleanupInputStream, IOUtil, S3Client}

import java.io.{BufferedInputStream, FileInputStream, InputStream}
import scala.collection.convert.ImplicitConversions._

class S3FileRecordFactory(location: String, endpoint: String, accessKey: String, secretKey: String, bucket: String, longestPrefixMapping: Boolean)
    extends FileRecordFactory with LongestPrefixProbing {
  class S3FileRecord private[S3FileRecordFactory](
      val filename: String,
      val mime: String,
      val meta: FileMeta)
      extends FileRecord {
    override lazy val path: String = locateFile(filename)
    override def access: InputStream = accessFile(filename, resolve = false)
  }

  override def get(filename: String, mime: String, meta: FileMeta): FileRecord =
    new S3FileRecord(filename, mime, meta)

  private def s3[R](action: S3Client => R): R = {
    S3Client(endpoint, accessKey, secretKey).access(action)
  }

  def accessFile(
      filename: String,
      resolve: Boolean = true,
      accessContext: CollectionAccessContext): InputStream = {
    val path = if (resolve) locateFile(filename) + "/" + filename else filename
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

  def locateFile(filename: String): String = {
    if (longestPrefixMapping) location + "/" + locateLongestPrefix(filename) else location
  }

  private val prefixes = collection.mutable.Map.empty[String, Set[String]]
  protected def nextPrefixes(prefix: String): Set[String] = {
    prefixes.getOrElseUpdate(prefix, {
      s3(_.s3.listObjects(bucket, (location + "/" + prefix).stripSuffix("/")).getObjectSummaries).map(_.getKey).toSet
    })
  }
}

object S3FileRecordFactory {
  def apply(spec: InputSpec): S3FileRecordFactory = {
    for {
      endpoint <- spec.str("s3-endpoint")
      accessKey <- spec.str("s3-accessKey")
      secretKey <- spec.str("s3-secretKey")
      bucket <- spec.str("s3-bucket")
      location <- spec.str("data-location")
    } yield {
      val longestPrefixMapping = spec.str("data-path-mapping").contains("longest-prefix")
      new S3FileRecordFactory(location, endpoint, accessKey, secretKey, bucket, longestPrefixMapping)
    }
  }.getOrElse {
    throw new RuntimeException("No location URL specified.")
  }
}

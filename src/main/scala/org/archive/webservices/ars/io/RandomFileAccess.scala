package org.archive.webservices.ars.io

import com.amazonaws.services.s3.model.GetObjectRequest
import org.archive.webservices.ars.model.ArchCollection
import org.archive.webservices.ars.model.collections.CollectionSpecifics
import org.archive.webservices.sparkling.io.{CleanupInputStream, IOUtil, S3Client}

import java.io.{BufferedInputStream, FileInputStream, InputStream}
import java.net.URL
import java.util.Base64
import scala.collection.mutable

object RandomFileAccess {
  lazy val collectionSpecificsCache = mutable.Map.empty[String, Option[CollectionSpecifics]]

  def access(
      context: FileAccessContext,
      file: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    file.source.toLowerCase match {
      case "http" | "https" =>
        context.keyRing.forUrl(file.url) match {
          case Some((FileAccessKeyRing.AccessMethodS3, Array(accessKey, secretKey))) =>
            s3Access(context, file, offset, positions, accessKey, secretKey)
          case Some((FileAccessKeyRing.AccessMethodBasic, Array(user, pw))) =>
            httpAccess(
              context,
              file,
              offset,
              positions,
              basicUser = Some(user),
              basicPassword = Some(pw))
          case Some((FileAccessKeyRing.AccessMethodBasic, Array(pw))) =>
            httpAccess(context, file, offset, positions, basicPassword = Some(pw))
          case Some((FileAccessKeyRing.AccessMethodVault, Array(user, pw))) =>
            vaultAccess(
              context,
              file,
              offset,
              positions,
              username = Some(user),
              password = Some(pw))
          case Some((FileAccessKeyRing.AccessMethodVault, Array(pw))) =>
            vaultAccess(context, file, offset, positions, password = Some(pw))
          case None => httpAccess(context, file, offset, positions)
        }
      case "hdfs" | "" =>
        hdfsAccess(context, file, offset, positions)
      case _ =>
        file.source match {
          case s if ArchCollection.prefix(s).isDefined =>
            collectionAccess(context, file, offset, positions)
          case _ => throw new UnsupportedOperationException()
        }
    }
  }

  def vaultAccess(
      context: FileAccessContext,
      file: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)],
      username: Option[String] = None,
      password: Option[String] = None): InputStream = {
    val (url, userPw) = IOHelper.splitUserPwUrl(file.url, username, password)
    val cookie = userPw
      .map { case (user, pw) =>
        context.keyValueCache
          .get(Vault.sessionIdCacheKey(user))
          .map(_.asInstanceOf[String])
          .getOrElse {
            Vault.session(user, pw)
          }
      }
      .map(Vault.SessionIdCookie -> _)
    httpAccessUrl(url, offset, positions, cookie = cookie)
  }

  def httpAccess(
      context: FileAccessContext,
      file: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)],
      basicUser: Option[String] = None,
      basicPassword: Option[String] = None,
      cookie: Option[(String, String)] = None): InputStream = {
    val (url, userPw) = IOHelper.splitUserPwUrl(file.url, basicUser, basicPassword)
    httpAccessUrl(url, offset, positions, userPw, cookie)
  }

  def httpAccessUrl(
      url: String,
      offset: Long,
      positions: Iterator[(Long, Long)],
      basicUserPw: Option[(String, String)] = None,
      cookie: Option[(String, String)] = None): InputStream = {
    val connection = new URL(url).openConnection
    for ((user, pw) <- basicUserPw) {
      val authString = Base64.getEncoder.encodeToString(s"$user:$pw".getBytes)
      connection.setRequestProperty("Authorization", "Basic " + authString)
    }
    for ((k, v) <- cookie) connection.setRequestProperty("Cookie", s"$k=$v")
    val in = connection.getInputStream
    IOUtil.skip(in, offset)
    IOHelper.splitMergeInputStreams(in, positions, buffered = false)
  }

  def hdfsAccess(
      context: FileAccessContext,
      file: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    val in = context.hdfsIO.open(file.path, offset)
    IOHelper.splitMergeInputStreams(in, positions, buffered = false)
  }

  def collectionAccess(
      context: FileAccessContext,
      file: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)]): InputStream = {
    collectionSpecificsCache
      .getOrElseUpdate(file.source, CollectionSpecifics.get(file.source))
      .map { specifics =>
        specifics.randomAccess(context, specifics.inputPath, file, offset, positions)
      }
      .getOrElse(IOUtil.EmptyStream)
  }

  def s3Access(
      context: FileAccessContext,
      file: FilePointer,
      offset: Long,
      positions: Iterator[(Long, Long)],
      accessKey: String,
      secretKey: String): InputStream = {
    val urlSplit = file.url.split('/')
    val (endpoint, bucket, path) = (urlSplit.head, urlSplit(1), urlSplit.drop(2).mkString("/"))
    def s3[R](action: S3Client => R): R = {
      S3Client(endpoint, accessKey, secretKey).access(action)
    }
    val tmpFile = IOUtil.tmpFile
    try {
      if (offset >= 0 || positions.nonEmpty) {
        val rangeOffset = if (offset < 0) 0 else offset
        for ((o, l) <- positions) {
          val (start, end) = (rangeOffset + o, rangeOffset + o + l - 1)
          val getObjectRequest = new GetObjectRequest(bucket, path).withRange(start, end)
          val rangeTmpFile = IOUtil.tmpFile
          s3(_.transfers.download(getObjectRequest, rangeTmpFile))
          val rangeIn = new BufferedInputStream(new FileInputStream(rangeTmpFile))
          IOUtil.copy(
            new CleanupInputStream(rangeIn, rangeTmpFile.delete),
            IOUtil.fileOut(tmpFile, append = true))
        }
      } else {
        s3(_.transfers.download(bucket, path, tmpFile).waitForCompletion())
      }
    } catch {
      case _: Exception => tmpFile.delete()
    }
    val in = new BufferedInputStream(new FileInputStream(tmpFile))
    new CleanupInputStream(in, tmpFile.delete)
  }
}

package org.archive.webservices.ars.model

import _root_.io.circe.parser
import _root_.io.circe.syntax._
import org.archive.webservices.ars.io.IOHelper
import org.archive.webservices.sparkling.io.HdfsIO

import scala.collection.immutable.ListMap

case class DerivativeOutputCache(count: Int, size: Long, files: Iterator[DerivativeOutput])

object DerivativeOutputCache {
  case class CachedDerivativeOutput(
      filename: String,
      dir: String,
      fileType: String,
      mimeType: String,
      downloadName: String,
      size: Long,
      time: Long,
      lineCount: Long,
      checksums: Map[String, String])
      extends DerivativeOutput {
    override def prefixDownload(prefix: String): DerivativeOutput =
      copy(downloadName = IOHelper.escapePath(prefix) + filename)
  }

  def parseLine(line: String): Option[DerivativeOutput] = {
    parser.parse(line).toOption.map(_.hcursor).flatMap { cursor =>
      for {
        filename <- cursor.get[String]("filename").toOption
        dir <- cursor.get[String]("dir").toOption
        fileType <- cursor.get[String]("fileType").toOption
        mimeType <- cursor.get[String]("mimeType").toOption
        size <- cursor.get[Long]("size").toOption
        time <- cursor.get[Long]("time").toOption
        lineCount <- cursor.get[Long]("lineCount").toOption
        checksums = {
          val checksums = cursor.downField("checksums")
          checksums.keys.toIterator.flatten.flatMap { key =>
            checksums.get[String](key).toOption.map(key -> _)
          }.toMap
        }
      } yield CachedDerivativeOutput(
        filename,
        dir,
        fileType,
        mimeType,
        filename,
        size,
        time,
        lineCount,
        checksums)
    }
  }

  def parse(cacheFile: String): Option[DerivativeOutputCache] = {
    val lines = HdfsIO.iterLines(cacheFile)
    if (lines.hasNext) {
      val metadata = lines.next()
      parser.parse(metadata).toOption.map(_.hcursor).flatMap { cursor =>
        for {
          count <- cursor.get[Int]("count").toOption
          size <- cursor.get[Long]("size").toOption
        } yield DerivativeOutputCache(
          count,
          size, {
            lines.flatMap(parseLine)
          })
      }
    } else None
  }

  def write(files: Iterator[DerivativeOutput], cacheFile: String): Unit = {
    val tmpFile = cacheFile + ".tmp"
    var count = 0
    var size = 0L
    HdfsIO.writeLines(
      path = tmpFile, {
        files.map { file =>
          count += 1
          size += file.size
          ListMap(
            "filename" -> file.filename.asJson,
            "dir" -> file.dir.asJson,
            "fileType" -> file.fileType.asJson,
            "mimeType" -> file.mimeType.asJson,
            "size" -> file.size.asJson,
            "time" -> file.time.asJson,
            "lineCount" -> file.lineCount.asJson,
            "checksums" -> file.checksums.asJson).asJson.noSpaces
        }
      },
      overwrite = true)
    HdfsIO.writeLines(
      cacheFile, {
        Iterator(ListMap("count" -> count.asJson, "size" -> size.asJson).asJson.noSpaces) ++ {
          HdfsIO.iterLines(tmpFile)
        }
      },
      overwrite = true)
    HdfsIO.delete(tmpFile)
  }
}

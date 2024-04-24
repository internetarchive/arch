package org.archive.webservices.ars.model.collections.inputspecs

import io.circe.parser._
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.CollectionAccessContext
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.HdfsIO
import org.archive.webservices.sparkling.util.RddUtil

object MetaFilesSpecLoader extends InputSpecLoader {
  override def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = action({
    val recordFactory = FileRecordFactory(spec)
    val recordFactoryBc = Sparkling.sc.broadcast(recordFactory)
    for {
      mimeKey <- spec.str("meta-mime-key")
    } yield {
      val mapFile = pathMapping(spec)
      val accessContext = CollectionAccessContext.fromLocalArchConf
      Sparkling.initPartitions(loadMeta(spec)).mapPartitions { partition =>
        accessContext.init()
        val recordFactory = recordFactoryBc.value
        recordFactory.accessContext = accessContext
        partition.flatMap { case (filename, meta) =>
          for {
            mime <- meta.str(mimeKey)
          } yield recordFactory.get(mapFile(filename), mime, meta)
        }
      }
    }
  }.getOrElse {
    throw new RuntimeException("No meta filename and/or mime key specified.")
  })

  def pathMapping(spec: InputSpec): String => String = {
    spec
      .str("data-path-mapping")
      .map { case "same-prefix" =>
        spec
          .str("meta-suffix")
          .map { suffix =>
            (_: String).stripSuffix(suffix) + "*"
          }
          .getOrElse {
            throw new RuntimeException("No meta filename suffix specified.")
          }
      }
      .getOrElse {
        throw new UnsupportedOperationException()
      }
  }

  def loadMeta(spec: InputSpec): RDD[(String, FileMeta)] = {
    spec
      .str("meta-source")
      .flatMap {
        case "hdfs" => Some(loadMetaHdfs(spec))
        case _ => None
      }
      .getOrElse {
        throw new UnsupportedOperationException()
      }
  }

  def loadMetaHdfs(spec: InputSpec): RDD[(String, FileMeta)] = {
    spec
      .str("meta-glob")
      .map { glob =>
        val files = RddUtil.loadFilesLocality(glob)
        val excludePrefix = spec.str("meta-exclude-prefix")
        val filtered =
          if (excludePrefix.isEmpty) files
          else files.filter(!_.split('/').last.startsWith(excludePrefix.get))
        parseMeta(
          spec,
          filtered.map { file =>
            (file, HdfsIO.lines(file).mkString("\n"))
          })
      }
      .getOrElse {
        throw new RuntimeException("No meta location specified")
      }
  }

  def parseMeta(spec: InputSpec, rdd: RDD[(String, String)]): RDD[(String, FileMeta)] = {
    spec.str("meta-format") match {
      case Some(format) if format.startsWith("json") =>
        parseJson(rdd: RDD[(String, String)], format.endsWith("-fuzzy"))
      case Some("key-value") =>
        spec
          .str("meta-kv-separator")
          .map { separator =>
            val parsed = rdd.map { case (file, lines) =>
              (
                file,
                lines
                  .split('\n')
                  .flatMap { line =>
                    val separatorIdx = line.indexOf(separator)
                    if (separatorIdx < 0) None
                    else
                      Some {
                        val (key, value) = (line.take(separatorIdx), line.drop(separatorIdx + 1))
                        key -> value
                      }
                  }
                  .toMap)
            }
            spec.str("meta-value-format") match {
              case Some("json") =>
                val jsonRdd = parsed.map { case (file, map) =>
                  val jsonBody = map
                    .map { case (key, value) =>
                      s""""$key":$value"""
                    }
                    .mkString(",")
                  file -> ("{" + jsonBody + "}")
                }
                parseJson(jsonRdd, fuzzy = false)
              case Some(_) => throw new UnsupportedOperationException()
              case None =>
                parsed.map { case (file, map) =>
                  file -> FileMeta(map)
                }
            }
          }
          .getOrElse {
            throw new RuntimeException("No meta key-value separator specified")
          }
      case None => throw new UnsupportedOperationException()
    }
  }

  def parseJson(rdd: RDD[(String, String)], fuzzy: Boolean = false): RDD[(String, FileMeta)] = {
    val cleaned = if (fuzzy) {
      rdd.map { case (filename, content) =>
        var missingComma = false
        var inJson = false
        var json = content.trim
          .split('\n')
          .map { line =>
            var fixedLine = line.trim
            val quoted = fixedLine.startsWith("\"")
            val propertyLine = quoted || fixedLine.matches("^[^ ]+\\:.+")
            if (propertyLine) {
              inJson = true
              if (!quoted) {
                val colonIdx = fixedLine.indexOf(":")
                fixedLine = "\"" + fixedLine.take(colonIdx) + "\"" + fixedLine.drop(colonIdx)
              }
              if (missingComma) fixedLine = "," + fixedLine
            }
            missingComma = inJson && !fixedLine.endsWith(",")
            fixedLine
          }
          .mkString("\n")
        if (!json.startsWith("{")) json = "{" + json
        if (!json.endsWith("{")) json = json + "}"
        filename -> json
      }
    } else rdd
    cleaned.flatMap { case (filename, json) =>
      parse(json).toOption.map(_.hcursor).map { cursor =>
        filename -> FileMeta(cursor)
      }
    }
  }
}

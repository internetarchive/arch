package org.archive.webservices.ars.model.collections.inputspecs

import _root_.io.circe.parser._
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.FileAccessContext
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.collections.inputspecs.meta.{FileMetaData, FileMetaField}
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.util.RddUtil

import scala.io.Source

object FileSpecLoader extends InputSpecLoader {
  val specType = "files"

  val MimeKey = "data-mime"

  override def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = action({
    val recordFactory = FileRecordFactory(spec, default = HdfsFileRecordFactory)
    val recordFactoryBc = Sparkling.sc.broadcast(recordFactory)
    val accessContext = FileAccessContext.fromLocalArchConf
    (recordFactory.dataSourceType match {
      case HdfsFileRecordFactory.dataSourceType => loadHdfs(spec, recordFactory)
      case VaultFileRecordFactory.dataSourceType => loadVault(spec, recordFactory)
      case PetaboxFileRecordFactory.dataSourceType => loadPetabox(spec, recordFactory)
      case _ => throw new UnsupportedOperationException()
    }).mapPartitions { partition =>
      accessContext.init()
      val recordFactory = recordFactoryBc.value
      recordFactory.accessContext = accessContext
      partition.map { case (path, mime) =>
        recordFactory.get(path, mime, FileMetaData(
          FileMetaField("path", path),
          FileMetaField("mime", mime)
        ))
      }
    }
  })

  def dataMime(spec: InputSpec): Option[Either[String, Map[String, String]]] = {
    spec.str(MimeKey) match {
      case Some(mime) => Some(Left(mime))
      case None =>
        val cursor = spec.cursor.downField(MimeKey)
        val map = cursor.keys.toSeq.flatten.flatMap { key =>
          cursor.get[String](key).toOption.map(key -> _)
        }.toMap
        if (map.isEmpty) None else Some(Right(map))
    }
  }

  def setMime(files: Iterator[String], mime: Either[String, Map[String, String]]): Iterator[(String, String)] = {
    mime match {
      case Left(m) => files.map((_, m))
      case Right(map) =>
        files.flatMap { path =>
          path.split('/').last.split('.').drop(1).tails.map(_.mkString(".")).find(map.contains).map { ext =>
            (path, map(ext))
          }
        }
    }
  }

  def loadHdfs(spec: InputSpec, recordFactory: FileRecordFactory): RDD[(String, String)] = {
    for {
      location <- spec.str(InputSpec.DataLocationKey)
      mime <- dataMime(spec)
    } yield {
      val rdd = RddUtil.loadFilesLocality(location, setPartitionFiles = false)
      val mimeBc = rdd.sparkContext.broadcast(mime)
      rdd.mapPartitions { partition =>
        setMime(partition, mimeBc.value)
      }
    }
  }.getOrElse {
    throw new RuntimeException("No location and/or mime type(s) specified.")
  }

  def loadVault(spec: InputSpec, recordFactory: FileRecordFactory): RDD[(String, String)] = {
    val vault = recordFactory.asInstanceOf[VaultFileRecordFactory]
    val (resolved, remaining) = vault.iterateGlob(Set(spec.str("file-glob").getOrElse("**")))
    val partitions =
      (resolved.map { case (p, n) => (p, n.fileType) } ++ remaining.map { p => (p, None) }).toSeq
    val vaultBc = Sparkling.sc.broadcast(vault)
    RddUtil.parallelize(partitions).mapPartitions { partition =>
      val vault = vaultBc.value
      partition.flatMap { case (path, fileType) =>
        fileType match {
          case Some(t) => Iterator((path, t))
          case None =>
            vault.glob(path).flatMap { case (p, n) =>
              n.fileType.map((p, _))
            }
        }
      }
    }
  }

  def loadPetabox(spec: InputSpec, recordFactory: FileRecordFactory): RDD[(String, String)] = {
    for {
      itemName <- spec.str(InputSpec.DataLocationKey)
      mime <- dataMime(spec)
    } yield {
      val source = Source.fromURL(ArchConf.iaBaseUrl + s"/metadata/$itemName/metadata/mediatype")
      val isCollection = try {
        parse(source.mkString).toOption.flatMap(_.hcursor.get[String]("result").toOption).contains("collection")
      } finally source.close()
      if (isCollection) {
        // search items, foreach process item
        val collectionSource = Source.fromURL(ArchConf.iaBaseUrl + "/advancedsearch.php?rows=1000&output=json&q=collection:" + itemName)
        try {
          val items = parse(collectionSource.mkString).toOption.map(_.hcursor).toSeq.flatMap { cursor =>
            cursor.downField("response").downField("docs").values.toSeq.flatten.map(_.hcursor).flatMap { itemCursor =>
              itemCursor.get[String]("identifier").toOption
            }
          }
          val rdd = RddUtil.parallelize(items)
          val mimeBc = rdd.sparkContext.broadcast(mime)
          rdd.flatMap(petaboxFiles).mapPartitions { partition =>
            setMime(partition, mimeBc.value)
          }
        } finally collectionSource.close()
      } else {
        val files = setMime(petaboxFiles(itemName).toIterator, mime).toSeq
        RddUtil.parallelize(files)
      }
    }
  }.getOrElse {
    throw new RuntimeException("No location and/or mime type(s) specified.")
  }

  def petaboxFiles(itemName: String): Seq[String] = {
    val source = Source.fromURL(ArchConf.iaBaseUrl + s"/metadata/$itemName/metadata/files")
    try {
      parse(source.mkString).toOption.flatMap(_.hcursor.values).getOrElse(Seq.empty).map(_.hcursor).flatMap { file =>
        file.get[String]("filename").toOption.map(itemName + "/" + _)
      }
    } finally source.close()
  }.toSeq
}

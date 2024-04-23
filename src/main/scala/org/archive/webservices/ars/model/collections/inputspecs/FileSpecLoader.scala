package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.FileAccessContext
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.util.RddUtil

object FileSpecLoader extends InputSpecLoader {
  val SpecType = "files"
  val MimeKey = "data-mime"

  override def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = action({
    val recordFactory = FileRecordFactory(spec, default = HdfsFileRecordFactory)
    val recordFactoryBc = Sparkling.sc.broadcast(recordFactory)
    val accessContext = FileAccessContext.fromLocalArchConf
    (recordFactory.dataSourceType match {
      case HdfsFileRecordFactory.dataSourceType => loadHdfs(spec, recordFactory)
      case VaultFileRecordFactory.dataSourceType => loadVault(spec, recordFactory)
      case _ => throw new UnsupportedOperationException()
    }).mapPartitions { partition =>
      accessContext.init()
      val recordFactory = recordFactoryBc.value
      recordFactory.accessContext = accessContext
      val meta = FileMeta.empty
      partition.map { case (path, mime) =>
        recordFactory.get(path, mime, meta)
      }
    }
  })

  def loadHdfs(
      spec: InputSpec,
      recordFactory: FileRecordFactory): RDD[(String, String)] = {
    for {
      location <- spec.str(InputSpec.DataLocationKey)
      mime <- spec.str(MimeKey)
    } yield {
      RddUtil.loadFilesLocality(location).mapPartitions { partition =>
        partition.map((_, mime))
      }
    }
  }.getOrElse {
    throw new RuntimeException("No location and/or mime type specified.")
  }

  def loadVault(
      spec: InputSpec,
      recordFactory: FileRecordFactory): RDD[(String, String)] = {
    val vault = recordFactory.asInstanceOf[VaultFileRecordFactory]
    val (resolved, remaining) = vault.iterateGlob(Set(spec.str("file-glob").getOrElse("**")))
    val partitions = (resolved.map { case (p, n) => (p, n.fileType) } ++ remaining.map { p => (p, None) }).toSeq
    val vaultBc = Sparkling.sc.broadcast(vault)
    RddUtil.parallelize(partitions).mapPartitions { partition =>
      val vault = vaultBc.value
      partition.flatMap { case (path, fileType) =>
        fileType match {
          case Some(t) => Iterator((path, t))
          case None => vault.glob(path).flatMap { case (p, n) =>
            n.fileType.map((p, _))
          }
        }
      }
    }
  }
}

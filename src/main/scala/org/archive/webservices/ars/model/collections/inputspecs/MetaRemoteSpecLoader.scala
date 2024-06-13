package org.archive.webservices.ars.model.collections.inputspecs
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.archive.webservices.ars.io.FileAccessContext
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaData
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil}

object MetaRemoteSpecLoader extends InputSpecLoader {
  val specType = "meta-remote"

  override def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = action({
    val recordFactory = FileRecordFactory(spec)
    val recordFactoryBc = Sparkling.sc.broadcast(recordFactory)
    for {
      filenameKey <- spec.str("meta-filename-key")
      mimeKey <- spec.str("meta-mime-key")
    } yield {
      val accessContext = FileAccessContext.fromLocalArchConf
      Sparkling.initPartitions(loadMeta(spec)).mapPartitions { partition =>
        accessContext.init()
        val recordFactory = recordFactoryBc.value
        recordFactory.accessContext = accessContext
        partition.flatMap { meta =>
          for {
            filename <- meta.str(filenameKey)
            mime <- meta.str(mimeKey)
          } yield recordFactory.get(filename, mime, meta)
        }
      }
    }
  }.getOrElse {
    throw new RuntimeException("No meta filename and/or mime key specified.")
  })

  def loadMeta(spec: InputSpec): RDD[FileMetaData] = {
    spec
      .str(InputSpec.MetaSourceKey)
      .orElse(spec.str(InputSpec.DataSourceKey))
      .flatMap {
        case HdfsFileRecordFactory.dataSourceType => Some(loadMetaHdfs(spec))
        case VaultFileRecordFactory.dataSourceType => Some(loadMetaVault(spec))
        case _ => None
      }
      .getOrElse {
        throw new UnsupportedOperationException()
      }
  }

  def loadMetaHdfs(spec: InputSpec): RDD[FileMetaData] = {
    spec
      .str(InputSpec.MetaLocationKey)
      .map {
        case location if location.endsWith(".parquet") =>
          loadParquet(location)
        case _ => throw new UnsupportedOperationException()
      }
      .getOrElse {
        throw new RuntimeException("No meta location specified")
      }
  }

  def loadMetaVault(spec: InputSpec): RDD[FileMetaData] = {
    spec
      .str(InputSpec.MetaLocationKey)
      .map {
        case location if location.endsWith(".parquet") =>
          val in = VaultFileRecordFactory(spec).accessFile(location)
          val tmpFile = HdfsIO.createTmpPath()
          val out = HdfsIO.out(tmpFile)
          try {
            IOUtil.copy(in, out)
          } finally {
            out.close()
          }
          loadParquet(tmpFile)
        case _ => throw new UnsupportedOperationException()
      }
      .getOrElse {
        throw new RuntimeException("No meta location specified")
      }
  }

  def loadParquet(path: String): RDD[FileMetaData] = {
    val dataFrame = SparkSession.builder.getOrCreate.read.parquet(path)
    val schema = Sparkling.sc.broadcast(dataFrame.schema)
    dataFrame.rdd.map { row =>
      FileMetaData.fromParquet(schema.value, row)
    }
  }
}

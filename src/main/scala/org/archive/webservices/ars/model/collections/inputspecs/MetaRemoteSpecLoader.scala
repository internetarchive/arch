package org.archive.webservices.ars.model.collections.inputspecs
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.archive.webservices.ars.io.CollectionAccessContext
import org.archive.webservices.sparkling.Sparkling

object MetaRemoteSpecLoader extends InputSpecLoader {
  override def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = action({
    val recordFactory = FileRecordFactory[Map[String, Any]](spec)
    val recordFactoryBc = Sparkling.sc.broadcast(recordFactory)
    for {
      filenameKey <- spec.str("meta-filename-key")
      mimeKey <- spec.str("meta-mime-key")
    } yield {
      val accessContext = CollectionAccessContext.fromLocalArchConf
      Sparkling.initPartitions(loadMeta(spec)).mapPartitions { partition =>
        accessContext.init()
        val recordFactory = recordFactoryBc.value
        recordFactory.accessContext = accessContext
        partition.flatMap { meta =>
          for {
            filename <- meta.get(filenameKey).filter(_ != null).map(_.toString)
            mime <- meta.get(mimeKey).filter(_ != null).map(_.toString)
          } yield {
            recordFactory.get(filename, mime, meta)
          }
        }
      }
    }
  }.getOrElse {
    throw new RuntimeException("No meta filename and/or mime key specified.")
  })

  def loadMeta(spec: InputSpec): RDD[Map[String, Any]] = {
    spec.str("meta-source").flatMap {
      case "hdfs" => Some(loadMetaHdfs(spec))
      case _ => None
    }.getOrElse {
      throw new UnsupportedOperationException()
    }
  }

  def loadMetaHdfs(spec: InputSpec): RDD[Map[String, Any]] = {
    spec.str("meta-location").map {
      case location if location.endsWith(".parquet") =>
        val dataFrame = SparkSession.builder.getOrCreate.read.parquet(location)
        val schema = Sparkling.sc.broadcast(dataFrame.schema)
        dataFrame.rdd.map {
          _.getValuesMap[Any](schema.value.fieldNames)
        }
      case _ => throw new UnsupportedOperationException()
    }.getOrElse {
      throw new RuntimeException("No meta location specified")
    }
  }
}

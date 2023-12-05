package org.archive.webservices.ars.model.collections.filespecs
import io.circe.HCursor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.CollectionAccessContext
import org.apache.spark.sql.SparkSession
import org.archive.webservices.sparkling.Sparkling

object MetaRemoteSpec extends FileSpec {
  override def load(spec: HCursor): RDD[FileRecord] = {
    val recordFactory = FileRecordFactory[Map[String, Any]](spec)
    val recordFactoryBc = Sparkling.sc.broadcast(recordFactory)
    for {
      filenameKey <- spec.get[String]("meta-filename-key").toOption
      mimeKey <- spec.get[String]("meta-mime-key").toOption
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
  }

  def loadMeta(spec: HCursor): RDD[Map[String, Any]] = {
    spec.get[String]("meta-source").toOption.flatMap {
      case "hdfs" => Some(loadMetaHdfs(spec))
      case _ => None
    }.getOrElse {
      throw new UnsupportedOperationException()
    }
  }

  def loadMetaHdfs(spec: HCursor): RDD[Map[String, Any]] = {
    spec.get[String]("meta-location").toOption.map {
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

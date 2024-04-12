package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.broadcast.Broadcast
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
    recordFactory.dataSourceType match {
      case HdfsFileRecordFactory.dataSourceType => loadHdfs(spec, recordFactoryBc)
      case _ => throw new UnsupportedOperationException()
    }
  })

  def loadHdfs(spec: InputSpec, recordFactoryBc: Broadcast[FileRecordFactory]): RDD[FileRecord] = {
    for {
      location <- spec.str(InputSpec.DataLocationKey)
      mime <- spec.str(MimeKey)
    } yield {
      val accessContext = FileAccessContext.fromLocalArchConf
      RddUtil.loadFilesLocality(location).mapPartitions { partition =>
        accessContext.init()
        val recordFactory = recordFactoryBc.value
        recordFactory.accessContext = accessContext
        val meta = FileMeta.empty
        partition.map { path =>
          recordFactory.get(path, mime, meta)
        }
      }
    }
  }.getOrElse {
    throw new RuntimeException("No location and/or mime type specified.")
  }
}

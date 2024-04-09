package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{FileAccessContext, WebArchiveLoader}
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.util.RddUtil

object FileSpecLoader extends InputSpecLoader {
  override def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = action({
    val recordFactory = FileRecordFactory(spec)
    val recordFactoryBc = Sparkling.sc.broadcast(recordFactory)
    recordFactory.dataSourceType match {
      case HdfsFileRecordFactory.dataSourceType => loadHdfs(spec, recordFactoryBc)
      case _ => throw new UnsupportedOperationException()
    }
  })

  def loadHdfs(spec: InputSpec, recordFactoryBc: Broadcast[FileRecordFactory]): RDD[FileRecord] = {
    val locationMime = {
      for {
        location <- spec.str(InputSpec.DataLocationKey)
        mime <- spec.str("data-mime")
      } yield (location, mime)
    }.orElse {
      for {
        uuid <- spec.str("data-cdx-uuid")
        location <- ArchConf.uuidJobOutPath.map(_ + "/" + uuid + UserDefinedQuery.relativeOutPath)
      } yield (location, WebArchiveLoader.CdxMime)
    }
    locationMime.map { case (location, mime) =>
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

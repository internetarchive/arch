package org.archive.webservices.ars.model.collections.inputspecs
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.FileAccessContext
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaData
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.util.RddUtil

object DatasetSpecLoader extends InputSpecLoader {
  val specType = "dataset"

  override def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.toFileSpec
      .map { fileSpec =>
        FileSpecLoader.loadSpark(fileSpec)(action)
      }
      .getOrElse {
        val recordFactoryBc = Sparkling.sc.broadcast(HdfsFileRecordFactory())
        val accessContext = FileAccessContext.fromLocalArchConf
        action(RddUtil.parallelize(spec.dataset.outFiles.toSeq).mapPartitions { partition =>
          accessContext.init()
          val recordFactory = recordFactoryBc.value
          recordFactory.accessContext = accessContext
          val meta = FileMetaData.empty
          partition.map { file =>
            recordFactory.get(file.path, file.mimeType, meta)
          }
        })
      }
  }

  override def size(spec: InputSpec): Long = spec.dataset.outputSize
}

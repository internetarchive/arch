package org.archive.webservices.ars.model.collections.inputspecs
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.FileAccessContext
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.util.RddUtil

object DatasetSpecLoader extends InputSpecLoader {
  override def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    spec.toFileSpec.map { fileSpec =>
      FileSpecLoader.load(fileSpec)(action)
    }.getOrElse {
      val recordFactoryBc = Sparkling.sc.broadcast(HdfsFileRecordFactory())
      val accessContext = FileAccessContext.fromLocalArchConf
      action(RddUtil.parallelize(spec.dataset.outFiles.toSeq).mapPartitions { partition =>
        accessContext.init()
        val recordFactory = recordFactoryBc.value
        recordFactory.accessContext = accessContext
        val meta = FileMeta.empty
        partition.map { file =>
          recordFactory.get(file.path, file.mimeType, meta)
        }
      })
    }
  }
}

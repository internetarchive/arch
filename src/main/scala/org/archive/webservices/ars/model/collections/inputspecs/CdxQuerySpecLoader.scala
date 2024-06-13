package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.WebArchiveLoader
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery

object CdxQuerySpecLoader extends InputSpecLoader {
  override def specType: String = "cdx-query"

  def input(spec: InputSpec): InputSpec = {
    spec.cursor
      .downField("input")
      .focus
      .map(json => InputSpec(json.hcursor))
  }.getOrElse {
    throw new UnsupportedOperationException("No sub spec specified.")
  }

  override def size(spec: InputSpec): Long = Some(super.size(spec)).filter(_ != -1).getOrElse {
    input(spec).size
  }

  override def inputType(spec: InputSpec): Option[String] = Some(InputSpec.InputType.CDX)

  override def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = action({
    for {
      query <- spec.params("query")
    } yield {
      for {
        error <- UserDefinedQuery.validateQuery(query)
      } throw new RuntimeException(error)

      WebArchiveLoader.loadCdx(input(spec)) { rdd =>
        val queryBc = rdd.sparkContext.broadcast(query)
        rdd.mapPartitionsWithIndex { (idx, partition) =>
          val cdx = UserDefinedQuery.filterQuery(partition, queryBc.value)
          Iterator(InMemoryCdxFileRecord(idx, cdx).asInstanceOf[FileRecord])
        }
      }
    }
  }.getOrElse {
    throw new UnsupportedOperationException("missing query")
  })
}

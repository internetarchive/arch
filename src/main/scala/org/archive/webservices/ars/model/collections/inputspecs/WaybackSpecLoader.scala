package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.io.{FileAccessContext, RandomFileAccess}
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery
import org.archive.webservices.sparkling.cdx.CdxServerIndex
import org.archive.webservices.sparkling.util.IteratorUtil
import org.archive.webservices.sparkling.{AccessContext, Sparkling}

object WaybackSpecLoader extends InputSpecLoader {
  val specType = "wayback"
  val cdxDir = "/user/wmdata2/cdx-all-index"

  override def inputType(spec: InputSpec): Option[String] = Some(InputSpec.InputType.CDX)

  override def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = action({
    for {
      cdxTimestamp <- spec.str("cdx-timestamp")
    } yield {
      val prefixes = spec.str("surt-prefix").map(Set(_)).orElse {
        spec.downField("surt-prefixes").flatMap(_.asArray).map(_.flatMap(_.asString).toSet).orElse {
          spec.str("url-prefix").map(CdxServerIndex.urlToPrefix(_)).orElse {
            spec.downField("url-prefixes").flatMap(_.asArray).map(_.flatMap(_.asString).flatMap { prefix =>
              CdxServerIndex.urlToPrefix(prefix)
            }.toSet)
          }
        }
      }.getOrElse {
        throw new UnsupportedOperationException("SURT/URL prefixes missing.")
      }

      val query = spec.params("filter-query")
      for {
        q <- query
        error <- UserDefinedQuery.validateQuery(q)
      } throw new RuntimeException(error)

      val select = spec.str("per-surt-select").map(_.toLowerCase)
      val selectFirst = select.contains("first")
      val selectLast = select.contains("last")

      implicit val sparklingAccessContext: AccessContext = {
        val accessContext = FileAccessContext.fromLocalArchConf
        AccessContext(accessContext.aitHdfsIO)
      }

      val rdd = if (selectFirst || selectLast) {
        val all = CdxServerIndex.loadFromFilesGroupedByPrefix(cdxDir + "/" + cdxTimestamp, prefixes)
        val filtered = query match {
          case Some(q) =>
            val queryBc = all.sparkContext.broadcast(q)
            all.map { case (_, records) =>
              UserDefinedQuery.filterQuery(records, queryBc.value)
            }
          case None => all.map(_._2)
        }
        if (selectFirst) {
          filtered.filter(_.hasNext).map(_.next)
        } else {
          filtered.flatMap(IteratorUtil.last(_))
        }
      } else {
        val all = CdxServerIndex.loadFromFiles(cdxDir + "/" + cdxTimestamp, prefixes)
        query match {
          case Some(q) =>
            val queryBc = all.sparkContext.broadcast(q)
            all.mapPartitions { partition =>
              UserDefinedQuery.filterQuery(partition, queryBc.value)
            }
          case None => all
        }
      }

      rdd.mapPartitionsWithIndex { (idx, partition) =>
        val cdx = partition.map { record =>
          val (location, offset) = record.locationFromAdditionalFields
          record.copy(additionalFields = Seq(offset.toString, RandomFileAccess.PetaboxPrefix + ":" + location))
        }
        Iterator(InMemoryCdxFileRecord(idx, cdx).asInstanceOf[FileRecord])
      }.coalesce(Sparkling.parallelism)
    }
  }.getOrElse {
    throw new UnsupportedOperationException("missing cdx-timestamp")
  })
}

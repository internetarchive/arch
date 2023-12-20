package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD
import org.archive.webservices.sparkling.util.RddUtil

object UnionSpecLoader extends InputSpecLoader {
  override def load[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    val specs = spec.cursor.downField("specs").values.toIterator.flatten.map(json => InputSpec(json.hcursor))
    var union = RddUtil.emptyRDD[FileRecord]
    def next: R = {
      if (specs.hasNext) {
        InputSpecLoader.load(specs.next) { rdd =>
          union = union.union(rdd)
          next
        }
      } else action(union)
    }
    next
  }
}

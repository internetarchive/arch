package org.archive.webservices.ars.model.collections.inputspecs

import org.apache.spark.rdd.RDD
import org.archive.webservices.sparkling.util.RddUtil

object MultiSpecLoader extends InputSpecLoader {
  override def specType: String = "multiSpecs"

  def multiSpecs(spec: InputSpec): Iterator[InputSpec] = {
    spec.cursor
      .downField("specs")
      .values
      .toIterator
      .flatten
      .map(json => InputSpec(json.hcursor))
  }

  override def size(spec: InputSpec): Long = Some(super.size(spec)).filter(_ != -1).getOrElse {
    val sizes = multiSpecs(spec).map(_.size).filter(_ != -1)
    if (sizes.isEmpty) -1 else sizes.sum
  }

  override def inputType(spec: InputSpec): Option[String] = {
    val types = multiSpecs(spec).map(_.inputType).toSet
    Some(if (types.size == 1) types.head else InputSpec.InputType.Files)
  }

  private def unionSpark[R](spec: InputSpec, load: InputSpec => (RDD[FileRecord] => R) => R)(
      action: RDD[FileRecord] => R): R = {
    val specs = multiSpecs(spec)
    var union = RddUtil.emptyRDD[FileRecord]
    def next: R = {
      if (specs.hasNext) {
        load(specs.next) { rdd =>
          union = union.union(rdd)
          next
        }
      } else action(union)
    }
    next
  }

  override def loadFilesSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    unionSpark[R](spec, InputSpecLoader.loadFilesSpark)(action)
  }

  override def loadSpark[R](spec: InputSpec)(action: RDD[FileRecord] => R): R = {
    unionSpark[R](spec, InputSpecLoader.loadSpark)(action)
  }
}

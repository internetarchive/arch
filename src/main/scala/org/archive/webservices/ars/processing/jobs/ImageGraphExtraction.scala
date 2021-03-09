package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.ImageGraphExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object ImageGraphExtraction extends AutJob {
  val name = "Extract image graph"
  val category = ArsCloudJobCategories.ImageData
  val description =
    "Create a CSV with the following columns: crawl date, source of the image (where it was hosted), the URL of the image, and the alternative text of the image."

  val targetFile: String = "image-graph.csv.gz"

  def df(rdd: RDD[ArchiveRecord]) = ImageGraphExtractor(rdd.imagegraph())
}

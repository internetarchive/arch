package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.ImageInformationExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object ImageInformationExtraction extends AutJob {
  val name = "Extract image information"
  val category = ArsCloudJobCategories.ImageData
  val description =
    "Create a CSV with the following columns: crawl date, URL of the image, filename, image extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, image width, image height, image MD5 hash and image SHA1 hash."

  val targetFile: String = "image-information.csv.gz"

  def df(rdd: RDD[ArchiveRecord]) = ImageInformationExtractor(rdd.images())
}

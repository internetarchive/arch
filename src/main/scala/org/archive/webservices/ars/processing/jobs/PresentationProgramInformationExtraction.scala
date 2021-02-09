package org.archive.webservices.ars.processing.jobs

import io.archivesunleashed.ArchiveRecord
import io.archivesunleashed.app.PresentationProgramInformationExtractor
import org.apache.spark.rdd.RDD
import org.archive.webservices.ars.model.ArsCloudJobCategories
import org.archive.webservices.ars.processing.jobs.shared.AutJob

object PresentationProgramInformationExtraction extends AutJob {
  val name = "Extract presentation program information"
  val category = ArsCloudJobCategories.BinaryInformation
  val description =
    "This will output a single file with the following columns: crawl date, URL of the presentation program file, filename, presentation program extension, MIME type as provided by the web server, MIME type as detected by Apache TIKA, presentation program MD5 hash and presentation program SHA1 hash."

  val targetFile: String = "presentation-program-information.csv.gz"

  def df(rdd: RDD[ArchiveRecord]) =
    PresentationProgramInformationExtractor(rdd.presentationProgramFiles())
}

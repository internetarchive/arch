package addon

import org.archive.webservices.ars.addons._
import org.archive.webservices.ars.processing.JobManager
import jobs._

object Addon extends ArchAddon {
  def initAddon(): Unit = {
    JobManager.userJobs += QualityTextExtraction
    JobManager.userJobs += TextExtraction
  }

  AddonLoader.init(this)
}
package org.archive.webservices.ars.processing.jobs.shared

import org.archive.webservices.ars.WasapiController
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.{DerivationJob, DerivationJobConf}

trait ArsJob extends DerivationJob {
  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] =
    super.templateVariables(conf) ++ Seq(
      "wasapiUrl" -> (ArchConf.baseUrl + "/ait/wasapi/v1/jobs/" + id + "/result?collection=" + conf.collectionId + (if (conf.isSample)
                                                                                                                                 "&sample=true"
                                                                                                                               else
                                                                                                                                 "")),
      "wasapiPages" -> (outFiles(conf).size.toDouble / WasapiController.FixedPageSize).ceil.toInt)
}

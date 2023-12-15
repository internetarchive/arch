package org.archive.webservices.ars.processing.jobs.shared

import org.archive.webservices.ars.WasapiController
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.{DerivationJob, DerivationJobConf}

trait ArsJob extends DerivationJob {
  override def templateVariables(conf: DerivationJobConf): Seq[(String, Any)] = {
    val wasapiUrl = ArchConf.baseUrl + {
      "/wasapi/v1/jobs/" + id + "/result?collection=" + conf.inputSpec.collectionId + {
        if (conf.isSample) "&sample=true" else ""
      }
    }
    super.templateVariables(conf) ++ Seq(
      "wasapiUrl" -> wasapiUrl,
      "wasapiPages" -> (outFiles(conf).size.toDouble / WasapiController.FixedPageSize).ceil.toInt)
  }
}

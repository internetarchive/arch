package org.archive.webservices.ars.model.api

import org.scalatra.swagger.annotations.ApiModelProperty

import org.archive.webservices.ars.model.PublishedDatasets
import org.archive.webservices.ars.processing.DerivationJob

case class AvailableJob(
    @ApiModelProperty(description = "Unique job type identifier") uuid: String,
    name: String,
    description: String,
    @ApiModelProperty(description = "Whether the job output is publishable to archive.org")
    publishable: Boolean,
    @ApiModelProperty(description = "Whether the job is internal/non-user-facing use only")
    internal: Boolean,
    @ApiModelProperty(description = "A link to the job source code") codeUrl: String,
    @ApiModelProperty(description = "A link to information about the job") infoUrl: String)
    extends ApiResponseObject[AvailableJob]

object AvailableJob {
  def apply(job: DerivationJob, isInternal: Boolean): AvailableJob =
    AvailableJob(
      uuid = job.uuid.toString,
      name = job.name,
      description = job.description,
      publishable = (!PublishedDatasets.ProhibitedJobs.contains(job)),
      internal = isInternal,
      codeUrl = job.codeUrl,
      infoUrl = job.infoUrl)
}

package org.archive.webservices.ars.model.api

import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory}
import org.archive.webservices.ars.processing.DerivationJob

case class AvailableJobsCategory(
    categoryName: String,
    categoryDescription: String,
    jobs: Seq[AvailableJob])
    extends ApiResponseObject[AvailableJobsCategory]

object AvailableJobsCategory {
  def apply(category: ArchJobCategory, jobs: Seq[DerivationJob]): AvailableJobsCategory = {
    val isInternal = (category == ArchJobCategories.System)
    AvailableJobsCategory(
      categoryName = category.name,
      categoryDescription = category.description,
      jobs = jobs.map(j => AvailableJob.apply(j, isInternal)))
  }
}

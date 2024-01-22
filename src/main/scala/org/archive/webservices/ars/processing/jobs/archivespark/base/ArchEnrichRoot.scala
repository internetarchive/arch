package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.archive.webservices.archivespark.model.TypedEnrichRoot

trait ArchEnrichRoot[+Meta] extends TypedEnrichRoot[Meta] {
  def mime: String
}

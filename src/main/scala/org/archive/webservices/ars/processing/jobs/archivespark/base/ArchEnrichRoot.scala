package org.archive.webservices.ars.processing.jobs.archivespark.base

import org.archive.webservices.archivespark.model.TypedEnrichRoot
import org.archive.webservices.archivespark.model.dataloads.{ByteLoad, TextLoad}
import org.archive.webservices.ars.model.collections.inputspecs.meta.FileMetaData

trait ArchEnrichRoot[+Meta] extends TypedEnrichRoot[Meta]
  with FileLoad.Root
  with ByteLoad.Root
  with TextLoad.Root
  with PlainTextLoad.Root
  with LocalFileCache {
  def mime: String
  def meta: FileMetaData
}

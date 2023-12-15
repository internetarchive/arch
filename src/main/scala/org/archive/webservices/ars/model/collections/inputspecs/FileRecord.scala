package org.archive.webservices.ars.model.collections.inputspecs

import java.io.InputStream

trait FileRecord {
  def filename: String
  def mime: String
  def path: String
  def access: InputStream
  def meta[A](key: String): Option[A] = None
}

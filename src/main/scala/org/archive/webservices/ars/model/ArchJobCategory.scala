package org.archive.webservices.ars.model

case class ArchJobCategory(name: String, description: String) {
  override def toString: String = name
}

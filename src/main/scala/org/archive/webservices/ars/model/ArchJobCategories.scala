package org.archive.webservices.ars.model

object ArchJobCategories {
  val None = ArchJobCategory("", "")

  val BinaryInformation = ArchJobCategory(
    "File Formats",
    "Find, describe, and use the files contained within a web archive, based on their format.")

  val Collection = ArchJobCategory(
    "Collection",
    "Discover domain-related patterns and high level information about the documents in a web archive.")

  val Network = ArchJobCategory(
    "Network",
    "Explore connections in a web archive visually.")

  val Text = ArchJobCategory(
    "Text",
    "Extract and analyze a web archive as text.")

  val System = ArchJobCategory(
    "System",
    "Internal system jobs that are not meant to be exposed to users.")
}

package org.archive.webservices.ars.model.api

case class InputSpec(
    `type`: String,
    collectionId: Option[String],
    specs: Option[Seq[InputSpec]],
    inputType: Option[String],
    uuid: Option[String])

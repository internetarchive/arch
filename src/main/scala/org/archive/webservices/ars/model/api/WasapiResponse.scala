package org.archive.webservices.ars.model.api

case class WasapiResponseFile(
    filename: String,
    filetype: String,
    checksums: Map[String, String],
    locations: Seq[String],
    size: Long,
    collection: Option[String])
    extends ApiResponseObject[WasapiResponseFile]

case class WasapiResponse(
    count: Int,
    next: Option[String],
    previous: Option[String],
    files: Seq[WasapiResponseFile])
    extends ApiResponseObject[WasapiResponse]

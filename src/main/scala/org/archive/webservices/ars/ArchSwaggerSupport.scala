package org.archive.webservices.ars

import org.scalatra.swagger._
import org.scalatra.util.NotNothing

trait ArchSwaggerSupport extends SwaggerSupport {
  // apiOperation wrapper to add X-API-* header params to all endpoints.
  def apiOp[T: Manifest: NotNothing](name: String): SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[T](name)
      parameter headerParam[String]("X-API-USER").description(
        "The user for which this request is being made")
      parameter headerParam[String]("X-API-KEY").description(
        "An API key that's authorized to act on behalf of X-API-USER"))
}

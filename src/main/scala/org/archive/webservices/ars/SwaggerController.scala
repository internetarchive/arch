package org.archive.webservices.ars

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, NativeSwaggerBase, Swagger}


class SwaggerController(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object ArchApiInfo extends ApiInfo(
    "The ARCH API",
    "Docs for the ARCH API",
    "https://arch.archive-it.org",
    "arch@archive.org",
    "AGPL-3.0",
    "https://www.gnu.org/licenses/agpl-3.0.en.html"
)

class ArchApiSwagger extends Swagger(Swagger.SpecVersion, "2.0.0", ArchApiInfo)

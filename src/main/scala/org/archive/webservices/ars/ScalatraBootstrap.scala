package org.archive.webservices.ars

import org.scalatra.LifeCycle
import org.scalatra.swagger.ApiKey

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ArchApiSwagger
  swagger.addAuthorization(ApiKey("X-API-KEY"))

  override def init(context: ServletContext): Unit = {
    context.mount(new AdminController, "/admin/*")
    context.mount(new ApiController, "/api/*")
    context.mount(new JobUuidApiController, "/api/job/*")
    context.mount(new WasapiController, "/wasapi/*")
    context.mount(new FilesController, "/files/*")
    context.mount(new SwaggerController, "/api-docs")
  }
}

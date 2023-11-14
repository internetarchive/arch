package org.archive.webservices.ars

import org.scalatra.LifeCycle

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    context.mount(new ApiController, "/api/*")
    context.mount(new JobUuidApiController, "/api/job/*")
    context.mount(new WasapiController, "/wasapi/*")
    context.mount(new FilesController, "/files/*")
  }
}

package org.archive.webservices.ars

import javax.servlet.ServletContext
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    context.mount(new AdminController, "/admin/*")
    context.mount(new ApiController, "/api/*")
    context.mount(new DefaultController, "/*")
  }
}

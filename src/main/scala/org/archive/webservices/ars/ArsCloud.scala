package org.archive.webservices.ars

import org.archive.webservices.ars.model.ArsCloudConf
import org.archive.webservices.ars.processing.SparkJobManager
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object ArsCloud {
  val Port = 12341
  val BasePath = "/ait"
  val BaseUrl: String = ArsCloudConf.baseUrl + BasePath
  val BaseDir = "/research_services"

  def start(contextPath: String, port: Int): Unit = {
    SparkJobManager.init()

    val server = new Server(port)

    val context = new WebAppContext()
    context.setContextPath(contextPath)
    context.setResourceBase("webapp")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false")
    context.setInitParameter(
      ScalatraListener.LifeCycleKey,
      classOf[ScalatraBootstrap].getCanonicalName)
//    context.setInitParameter(org.scalatra.EnvironmentKey, "production")
    context.setEventListeners(Array(new ScalatraListener))

    server.setHandler(context)
    server.start()
    server.join()
  }

  def main(args: Array[String]): Unit = {
    start(BasePath, Port)
  }
}

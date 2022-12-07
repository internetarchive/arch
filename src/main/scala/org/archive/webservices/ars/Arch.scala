package org.archive.webservices.ars

import java.io.File

import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.JobStateManager
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.util.RddUtil
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object Arch {
  val Port: Int = ArchConf.port
  val BasePath = "/ait"
  val BaseUrl: String = ArchConf.baseUrl + BasePath
  val BaseDir = "/research_services"

  def start(contextPath: String, port: Int): Unit = {
    val server = new Server(port)

    val context = new WebAppContext()
    context.setContextPath(contextPath)
    context.setResourceBase("webapp")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false")
    context.setInitParameter(
      ScalatraListener.LifeCycleKey,
      classOf[ScalatraBootstrap].getCanonicalName)
    if (ArchConf.production) context.setInitParameter(org.scalatra.EnvironmentKey, "production")
    context.setEventListeners(Array(new ScalatraListener))

    server.setHandler(context)
    server.start()
    server.join()
  }

  def main(args: Array[String]): Unit = {
    IOUtil.memoryBuffer = 1.mb.toInt
    RddUtil.saveRecordTimeoutMillis = -1
    JobStateManager.init()
    start(BasePath, Port)
  }

  def debugging: Boolean = new File("_debugging").exists
}

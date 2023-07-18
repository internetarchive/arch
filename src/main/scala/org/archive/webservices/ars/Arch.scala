package org.archive.webservices.ars

import _root_.io.sentry.protocol.Message
import _root_.io.sentry.{Sentry, SentryEvent, SentryLevel}
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.processing.JobStateManager
import org.archive.webservices.sparkling._
import org.archive.webservices.sparkling.io.IOUtil
import org.archive.webservices.sparkling.util.RddUtil
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

import java.io.File
import scala.collection.JavaConversions._ // For SentryEvent.setExtras

object Arch {
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
    context.setInitParameter(org.scalatra.EnvironmentKey, ArchConf.deploymentEnvironment match {
      case "DEV" => "development"
      case "QA" => "qa"
      case "PROD" => "production"
    })
    context.setEventListeners(Array(new ScalatraListener))

    server.setHandler(context)
    server.start()
    server.join()
  }

  def initSentry(): Unit = {
    Sentry.init(options => {
      options.setDsn(ArchConf.sentryDsn);
      options.setEnvironment(ArchConf.deploymentEnvironment);
      // Set traces_sample_rate to 0.10 to capture 10% of transactions for performance monitoring.
      options.setTracesSampleRate(0.10);
    })
  }

  def reportEvent(
      title: String,
      message: String,
      extraContext: Map[String, Object] = Map.empty,
      level: SentryLevel = SentryLevel.INFO): Unit = {
    // Send an event to Sentry.
    val event = new SentryEvent()
    val _message = new Message()
    // Use the title as the message and add the message text as the extra "details" property
    // to make display in the client more reasonable, otherwise Sentry will display a prefix
    // of the message text as the event title.
    _message.setMessage(title)
    event.setMessage(_message)
    event.setLevel(level)
    event.setExtras(Map("details" -> message) ++ extraContext)
    Sentry.captureEvent(event)
  }

  val reportInfo = reportEvent(_, _, _, SentryLevel.INFO)
  val reportWarning = reportEvent(_, _, _, SentryLevel.WARNING)
  val reportError = reportEvent(_, _, _, SentryLevel.ERROR)

  def reportException(e: Exception): Unit = Sentry.captureException(e)

  def main(args: Array[String]): Unit = {
    IOUtil.memoryBuffer = 1.mb.toInt
    RddUtil.saveRecordTimeoutMillis = -1
    JobStateManager.init()
    initSentry()
    start(ArchConf.basePath, ArchConf.internalPort)
  }

  def debugging: Boolean = new File("_debugging").exists
}

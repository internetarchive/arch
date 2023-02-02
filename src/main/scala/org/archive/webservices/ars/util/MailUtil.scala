package org.archive.webservices.ars.util

import java.io.{File, PrintStream}
import scala.io.Source
import scala.util.Try

object MailUtil {
  val TemplatesDir = "templates"
  val TemplateExtension = ".txt"
  val TemplatePrefix = "sendmail_"

  def template(name: String, variables: => Map[String, String]): String = {
    Try {
      val file = new File(TemplatesDir, TemplatePrefix + name + TemplateExtension)
      if (file.exists && file.isFile) {
        val source = Source.fromFile(file, "utf-8")
        try {
          variables.foldLeft(source.mkString) {
            case (content, (variable, value)) => content.replace("$" + variable, value)
          }
        } finally {
          Try(source.close())
        }
      } else ""
    }.toOption.getOrElse("")
  }

  def send(message: String): Boolean = {
    val proc = Runtime.getRuntime.exec("sendmail -t")
    val writer = new PrintStream(proc.getOutputStream)
    writer.println(message)
    writer.close()
    while (proc.isAlive) Thread.`yield`()
    proc.exitValue == 0
  }

  def sendTemplate(name: String, variables: Map[String, String]): Boolean =
    send(template(name, variables))
}

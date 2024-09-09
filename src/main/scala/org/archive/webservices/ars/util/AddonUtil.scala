package org.archive.webservices.ars.util

import java.util.jar.JarFile
import scala.collection.JavaConverters._

object AddonUtil {
  def initializePackage(packageName: String): Unit = {
    println(s"Initializing add-ons of package $packageName...")
    val packagePath = packageName.replace('.', '/') + "/"
    val systemResources = ClassLoader.getSystemResources(packagePath).asScala
    val threadResources = Thread.currentThread.getContextClassLoader.getResources(packagePath).asScala;
    val jarEntries = (systemResources ++ threadResources).filter(_.getProtocol == "jar").flatMap { jar =>
      jar.getPath.stripPrefix("file:").split('!').headOption
    }.flatMap(path => new JarFile(path).entries().asScala).map(_.toString)
    val objects = jarEntries.filter(_.startsWith(packagePath)).filter(_.endsWith("$.class")).map(_.stripSuffix(".class").replace('/', '.'))
    for (objectClass <- objects) {
      val c = Class.forName(objectClass)
      println(s"Initialized add-on ${c.getCanonicalName}.")
    }
    println(s"Initialized add-ons of package $packageName.")
  }
}
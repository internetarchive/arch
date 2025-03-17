package org.archive.webservices.ars.addons

import java.util.jar.JarFile
import scala.collection.JavaConverters._

object AddonLoader {
  private var init: Map[String, ArchAddon] = Map.empty

  def init(addon: ArchAddon): Unit = {
    init += addon.getClass.getName -> addon
  }

  def initializePackage(packageName: String): Unit = {
    println(s"Initializing add-ons of package $packageName...")
    val packagePath = packageName.replace('.', '/') + "/"
    val systemClassLoader = ClassLoader.getSystemClassLoader
    val systemResources = systemClassLoader.getResources(packagePath).asScala
    val contextClassLoader = Thread.currentThread.getContextClassLoader
    val threadResources = contextClassLoader.getResources(packagePath).asScala
    val jarEntries = (systemResources ++ threadResources)
      .filter(_.getProtocol == "jar")
      .flatMap { jar =>
        jar.getPath.stripPrefix("file:").split('!').headOption
      }
      .flatMap(path => new JarFile(path).entries().asScala)
      .map(_.toString)
    val objects = jarEntries
      .filter(_.startsWith(packagePath))
      .filter(_.endsWith("$.class"))
      .map(_.stripSuffix(".class").replace('/', '.'))
    for (objectClass <- objects) {
      try {
        Class.forName(objectClass, true, systemClassLoader)
      } catch {
        case _: ClassNotFoundException =>
          Class.forName(objectClass, true, contextClassLoader)
      }
    }
    for (addon <- init.values) {
      println(s"Loading add-on ${addon.getClass.getName.stripSuffix("$")}...")
      addon.initAddon()
    }
    println(s"Initialized add-ons of package $packageName.")
  }
}

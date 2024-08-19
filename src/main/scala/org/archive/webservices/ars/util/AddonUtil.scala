package org.archive.webservices.ars.util

import java.util.jar.JarFile
import scala.collection.JavaConverters._

object AddonUtil {
  def initializePackage(packageName: String): Unit = {
    val packagePath = packageName.replace('.', '/') + "/"
    val jarEntries = ClassLoader.getSystemResources(packagePath).asScala.filter(_.getProtocol == "jar").flatMap { jar =>
      jar.getPath.stripPrefix("file:").split('!').headOption
    }.flatMap(path => new JarFile(path).entries().asScala).map(_.toString)
    val objects = jarEntries.filter(_.startsWith(packagePath)).filter(_.endsWith("$.class")).map(_.stripSuffix(".class").replace('/', '.'))
    for (objectClass <- objects) Class.forName(objectClass)
  }
}
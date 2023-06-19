package org.archive.webservices.ars

import org.scalatra.test.scalatest._
import org.scalamock.scalatest.MockFactory

/* Common Base Test Class */
abstract class UnitSpec extends ScalatraFunSuite with MockFactory {
  // https://stackoverflow.com/a/34030731
  def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }
}

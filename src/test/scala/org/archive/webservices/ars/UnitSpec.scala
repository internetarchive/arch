package test.org.archive.webservices.ars

import java.io.File

import org.apache.commons.io.FileUtils
import org.eclipse.jetty.server.Server
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}
import org.scalatra.test.scalatest._

import org.archive.webservices.sparkling.logging.LogLevels
import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.users.ArchUser

/* Common Base Test Class */
abstract class UnitSpec
    extends ScalatraSuite
    with FunSuiteLike
    with MockFactory
    with BeforeAndAfterAll {
  private val dataDir = "data"
  private val backupDataDir = ".data-test-bak"

  // Configure tests to use our non-standard webapp path.
  servletContextHandler.setResourceBase("webapp")

  override def beforeAll {
    super.beforeAll()
    // Assert that the configured environment is valid for testing.
    assert(ArchConf.deploymentEnvironment == "DEV")

    // Create a backup of the existing data directory.
    FileUtils.copyDirectory(new File(dataDir), new File(backupDataDir))

    // Set Sparkling logging level
    org.archive.webservices.sparkling.logging.Log.level = LogLevels.Error
  }

  override def afterAll {
    super.afterAll()
    // Restore the pre-existing data directory.
    FileUtils.copyDirectory(new File(backupDataDir), new File(dataDir))
  }

  // https://stackoverflow.com/a/34030731
  def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map =
      field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }

  def getForUser[A](user: ArchUser, apiKey: String)(
      uri: String,
      params: Iterable[(String, String)] = Seq.empty,
      headers: Iterable[(String, String)] = Seq.empty)(f: ⇒ A): A = {
    val headersKeys = headers.map(_._1.toLowerCase)
    var extraHeaders: Iterable[(String, String)] = (
      (if (!headersKeys.exists(_ == "x-api-user")) Seq(("x-api-user", user.id)) else Seq.empty)
        ++ (if (!headersKeys.exists(_ == "x-api-key")) Seq(("x-api-key", apiKey)) else Seq.empty)
    )
    get[A](uri, params, headers ++ extraHeaders)(f)
  }

  def postForUser[A](user: ArchUser, apiKey: String)(
      uri: String,
      body: Array[Byte],
      headers: Iterable[(String, String)] = Seq.empty)(f: => A): A = {
    val headersKeys = headers.map(_._1.toLowerCase)
    var extraHeaders: Iterable[(String, String)] = (
      (if (!headersKeys.exists(_ == "x-api-user")) Seq(("x-api-user", user.id)) else Seq.empty)
        ++ (if (!headersKeys.exists(_ == "x-api-key")) Seq(("x-api-key", apiKey)) else Seq.empty)
    )
    post(uri, body, headers ++ extraHeaders)(f)
  }

}

package test.org.archive.webservices.ars

import java.io.File

import org.apache.commons.io.FileUtils
import org.eclipse.jetty.server.Server
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}
import org.scalatra.test.scalatest._

import org.archive.webservices.ars.model.ArchConf
import org.archive.webservices.ars.model.users.ArchUser

/* Common Base Test Class */
abstract class UnitSpec extends ScalatraSuite with FunSuiteLike with MockFactory with BeforeAndAfterAll {
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

  def loggedInAs[A](user: ArchUser)(test: => A): A = {
    session {
      post("/login", params = Seq(("username", user.fullName), ("password", user.fullName))) {
        status should equal (302)
      }
      test
    }
  }
}

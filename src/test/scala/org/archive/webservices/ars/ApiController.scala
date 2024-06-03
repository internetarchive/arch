package test.org.archive.webservices.ars

import io.circe.parser.parse
import org.scalatra.test.scalatest._

import org.archive.webservices.ars.{ApiController, DefaultController}

import Fixtures._

class ApiControllerSpec extends UnitSpec {
  addServlet(classOf[DefaultController], "/*")
  addServlet(classOf[ApiController], "/api/*")

  test("/api/collections returns status 403 when not authenticated") {
    get("/api/collections") {
      status should equal (403)
    }
  }

  test("/api/collections returns status 200 and count=0 when no collections exist") {
    loggedInAs(makeArchUser()) {
      get("/api/collections") {
        status should equal (200)
        val cur = parse(body).right.get.hcursor
        cur.get[Int]("count").right.get should equal (0)
      }
    }
  }
}

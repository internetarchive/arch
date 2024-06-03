package test.org.archive.webservices.ars

import io.circe.parser.parse
import io.circe.syntax._
import org.scalatra.test.scalatest._

import org.archive.webservices.ars.{ApiController, DefaultController, JobUuidApiController}
import org.archive.webservices.ars.processing.jobs.DomainFrequencyExtraction

import Fixtures._

class JobUuidApiControllerSpec extends UnitSpec {
  addServlet(classOf[DefaultController], "/*")
  addServlet(classOf[JobUuidApiController], "/api/job/*")
  addServlet(classOf[ApiController], "/api/*")

  test("Generating a DomainFrequencyExtraction on the test collection works") {
    val user = makeArchUser()
    loggedInAs(user) {
      post(s"/api/runjob/${DomainFrequencyExtraction.id}?sample=false",
        parse(s"""
          {
            "user": "${user.userName}",
            "inputSpec": {
              "type": "collection",
              "collectionId": "SPECIAL-test-collection"
            },
            "params": {
              "dataset": "${DomainFrequencyExtraction.id}"
            }
          }
        """).toOption.get.toString.getBytes,
        Map("content-type" -> "application/json")
      ) {
        status should equal (200)
        val cur = parse(body).right.get.hcursor
        cur.get[String]("id").right.get should equal (DomainFrequencyExtraction.id)
        cur.get[String]("name").right.get should equal (DomainFrequencyExtraction.name)
        cur.get[Int]("sample").right.get should equal (-1)
        cur.get[String]("state").right.get should equal ("Running")
        cur.get[Boolean]("started").right.get should equal (true)
        cur.get[Boolean]("finished").right.get should equal (false)
        cur.get[Boolean]("failed").right.get should equal (false)
        cur.get[String]("activeStage").right.get should equal ("Processing")
        cur.get[String]("activeState").right.get should equal ("Running")

        val uuid = cur.get[String]("uuid").right.get

        Thread.sleep(15000)
      }
    }
  }
}

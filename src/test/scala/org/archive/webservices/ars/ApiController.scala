package test.org.archive.webservices.ars

import scala.util.Try
import io.circe.{Json, JsonObject}
import io.circe.parser.parse
import io.circe.syntax._
import org.scalatra.test.scalatest._
import org.archive.webservices.ars.{ApiController, ArchApiSwagger, JobUuidApiController}
import org.archive.webservices.ars.model.users.ArchUser
import org.archive.webservices.ars.processing.DerivationJob
import org.archive.webservices.ars.processing.jobs._
import org.archive.webservices.ars.processing.jobs.archivespark.preset._
import Fixtures._
import org.archive.webservices.ars.io.WebArchiveLoader
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec

import scala.collection.immutable.ListMap

class ApiControllerSpec extends UnitSpec {

  implicit val swagger = new ArchApiSwagger

  addServlet(new ApiController, "/api/*")
  addServlet(new JobUuidApiController, "/api/job/*")

  def parseJsonArray(json: String): Vector[Json] = parse(json).toOption.get.asArray.get
  def parseJsonObject(json: String): JsonObject = parse(json).toOption.get.asObject.get

  def runJob[A](
       user: ArchUser,
       apiKey: String,
       job: DerivationJob,
       inputSpec: Map[String, String],
       params: Map[String, String]): (=> A) => A = {
    runJob(user, apiKey, job, inputSpec.asJson,
      if (params.nonEmpty) Some(params.asJson) else None)
  }

  def runJob[A](
      user: ArchUser,
      apiKey: String,
      job: DerivationJob,
      inputSpec: Json,
      params: Option[Json] = None): (=> A) => A = {
    postForUser[A](user, apiKey)(
      uri = s"/api/runjob/${job.id}?sample=false",
      body = (ListMap(
        "user" -> user.userName.asJson,
        "inputSpec" -> inputSpec
      ) ++ params.map("params" -> _)).asJson.toString.getBytes,
      headers = Map("content-type" -> "application/json"))
  }

  def waitForJobComplete(
      user: ArchUser,
      apiKey: String,
      uuid: String,
      timeoutSeconds: Int = 120): Unit = {
    var secondsRemaining = timeoutSeconds;
    while (secondsRemaining > 0) {
      getForUser(user, apiKey)(s"/api/job/${uuid}/state") {
        status should equal(200)
        val respObj = parseJsonObject(body)
        val jobState = respObj("state").get.asString.get
        if (jobState == "Finished") {
          return
        }
        if (jobState == "Failed") fail(s"Job ($uuid) failed")
        Thread.sleep(1000)
        secondsRemaining -= 1
      }
    }
    fail(s"Timed out while waiting for job ($uuid) to complete")
  }

  def runJobAndVerify(
       user: ArchUser,
       apiKey: String,
       job: DerivationJob,
       inputSpec: Map[String, String],
       expectedFilesResponse: String,
       params: Map[String, String] = Map.empty
     ) {
    runJobAndVerify(user, apiKey, job, inputSpec.asJson, expectedFilesResponse,
      if (params.nonEmpty) Some(params.asJson) else None)
  }

  def runJobAndVerify(
    user: ArchUser,
    apiKey: String,
    job: DerivationJob,
    inputSpec: Json,
    expectedFilesResponse: String,
    params: Option[Json]
  ) {
    runJob(user, apiKey, job, inputSpec, params) {
      status should equal(200)
      val respObj = parseJsonObject(body)
      // Check only static response fields because we can't know whether the
      // job was started immediately, or queued, etc.
      val staticKeys = Set("id", "name", "sample", "failed")
      respObj.filterKeys(staticKeys.contains(_)) should equal(parseJsonObject(s"""
        {
          "id": "${job.id}",
          "name": "${job.name}",
          "sample": -1,
          "failed": false
        }
      """))

      val uuid = respObj("uuid").get.asString.get

      // Wait for the job to complete.
      waitForJobComplete(user, apiKey, uuid)

      // Check that the reported output files are as expected.
      var waitingForFilesCache = true
      while (waitingForFilesCache) {
        getForUser(user, apiKey)(s"/api/job/$uuid/files") {
          status should equal(200)
          // Response will either be an array of file objects, or { "lazy": true }.
          val filesResponseOpt = Try(parseJsonArray(body)).toOption
          if (filesResponseOpt.isDefined) {
            val expectedFilesResponseJson = parseJsonArray(expectedFilesResponse)
            // Get the set of keys defined by the first expected file object and assert
            // that the same set is defined by all.
            val includeKeys = expectedFilesResponseJson.head.asObject.get.keys.toSet
            (expectedFilesResponseJson.forall(_.asObject.get.keys.toSet == includeKeys)) should be (true)
            val actualFilesResponse = filesResponseOpt.get
              .map(json => json.mapObject(obj => obj.filterKeys(k => includeKeys.contains(k))))
            (actualFilesResponse) should equal (expectedFilesResponseJson)
            waitingForFilesCache = false
          } else {
            // Assert that non-array response was { "lazy": true } and then sleep before
            // trying again.
            (parseJsonObject(body)) should equal (parseJsonObject("""{ "lazy": true }"""))
            Thread.sleep(1000)
          }
        }
      }
    }
  }

  test("/api/collections returns status 403 when not authenticated") {
    get("/api/collections") {
      status should equal(403)
    }
  }

  test("/api/collections returns status 200 and count=0 when no collections exist") {
    val (user, apiKey) = makeArchUser()
    getForUser(user, apiKey)("/api/collections") {
      status should equal(200)
      val cur = parse(body).right.get.hcursor
      cur.get[Int]("count").right.get should equal(0)
    }
  }

  test("Generating a DomainFrequencyExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      DomainFrequencyExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename": "domain-frequency.csv.gz",
          "sizeBytes": 131,
          "mimeType": "application/gzip",
          "lineCount": 3,
          "fileType": "csv",
          "md5Checksum": "51e13b611a6fe2219faf9cc6da0be29c"
        }
      ]
      """
    )
  }

  test("Generating a DomainGraphExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      DomainGraphExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename": "domain-graph.csv.gz",
          "sizeBytes": 156,
          "mimeType": "application/gzip",
          "lineCount": 2,
          "fileType": "csv",
          "md5Checksum": "ff337ae7f3c20325b69358749a6c4f8c"
        }
      ]
      """
    )
  }

  test("Generating an ArchiveSpark EntityExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      EntityExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename": "result.jsonl.gz",
          "sizeBytes": 5494,
          "mimeType": "application/gzip",
          "lineCount": 41,
          "fileType": "ArchiveSpark/jsonl",
          "md5Checksum": "fe03006ab3d288804bb393f4efac3c07"
        }
      ]
      """
    )
  }

  test("Generating a ArsWatGeneration on the test collection works") {
    /*
     * The generated wat.gz file contains a job-run-specific timestamp
     * and randomly-generated WARC-Record-ID values so we can't reliably test
     * for size (due to content-specific compression differences) or MD5,
     * so let's just check the basics for now.
     */
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      ArsWatGeneration,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename": "test.warc.wat.gz",
          "mimeType": "application/gzip",
          "lineCount": -1,
          "fileType": "wat"
        }
      ]
      """
    )
  }

  test("Generating a ImageGraphExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      ImageGraphExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename": "image-graph.csv.gz",
          "sizeBytes": 1079,
          "mimeType": "application/gzip",
          "lineCount": 50,
          "fileType": "csv",
          "md5Checksum": "f6e38fe1dd47ebc226ce6ec6f0481bca"
        }
      ]
      """
    )
  }

  test("Generating a ArsLgaGeneration on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      ArsLgaGeneration,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "id.map.gz",
          "sizeBytes" : 4952,
          "mimeType" : "application/gzip",
          "lineCount" : -1,
          "fileType" : "lga-map",
          "md5Checksum" : "a1687f0527160db2ae23c78202628331"
        },
        {
          "filename" : "id.graph.gz",
          "sizeBytes" : 624,
          "mimeType" : "application/gzip",
          "lineCount" : -1,
          "fileType" : "lga-graph",
          "md5Checksum" : "bbbbecf95807c557c823e80ccfcfaa48"
        }
      ]
      """
    )
  }

  test("Generating a WebGraphExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      WebGraphExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "web-graph.csv.gz",
          "sizeBytes" : 10253,
          "mimeType" : "application/gzip",
          "lineCount" : 894,
          "fileType" : "csv",
          "md5Checksum" : "c11a8b1c40f101eb0019341fa0861bb0"
        }
      ]
      """
    )
  }

  test("Generating a WebPagesExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      WebPagesExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "web-pages.csv.gz",
          "sizeBytes" : 20735,
          "mimeType" : "application/gzip",
          "lineCount" : 17,
          "fileType" : "csv",
          "md5Checksum" : "d4b0ef3bf8afd88d2d72b9b2c40cf963"
        }
      ]
      """
    )
  }

  test("Generating an ArchiveSpark HtmlTextExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      HtmlTextExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "result.jsonl.gz",
          "sizeBytes" : 20725,
          "mimeType" : "application/gzip",
          "lineCount" : 17,
          "fileType" : "ArchiveSpark/jsonl",
          "md5Checksum" : "ec920d2b69f65846d1ff80d02a53990f"
        }
      ]
      """
    )
  }

  test("Generating an ArchiveSpark PySmokeTestJob on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      PySmokeTestJob,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "result.jsonl.gz",
          "sizeBytes" : 7310,
          "mimeType" : "application/gzip",
          "lineCount" : 80,
          "fileType" : "ArchiveSpark/jsonl",
          "md5Checksum" : "698e2b543c06d871414d94d62d6d050f"
        }
      ]
      """
    )
  }

  test("Generating a TextFilesInformationExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      TextFilesInformationExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "css-file-information.csv.gz",
          "sizeBytes" : 112843,
          "mimeType" : "application/gzip",
          "lineCount" : 11,
          "fileType" : "csv",
          "md5Checksum" : "4d34306cdfb43d2ffdbd57dda860758e"
          }, {
          "filename" : "json-file-information.csv.gz",
          "sizeBytes" : 1299,
          "mimeType" : "application/gzip",
          "lineCount" : 1,
          "fileType" : "csv",
          "md5Checksum" : "a5cd0ea93c4642f71ae7688deba7b30c"
          }, {
          "filename" : "xml-file-information.csv.gz",
          "sizeBytes" : 33701,
          "mimeType" : "application/gzip",
          "lineCount" : 7,
          "fileType" : "csv",
          "md5Checksum" : "fb3d19fb44880c93f6fbed3a4815bbb8"
          }, {
          "filename" : "plain-text-file-information.csv.gz",
          "sizeBytes" : 1924,
          "mimeType" : "application/gzip",
          "lineCount" : 14,
          "fileType" : "csv",
          "md5Checksum" : "2c31125c17d91e7803fe08fbb5111e75"
          }, {
          "filename" : "js-file-information.csv.gz",
          "sizeBytes" : 156098,
          "mimeType" : "application/gzip",
          "lineCount" : 13,
          "fileType" : "csv",
          "md5Checksum" : "fbea8902bcf7d55fa18dcbe868006702"
          }, {
          "filename" : "html-file-information.csv.gz",
          "sizeBytes" : 250333,
          "mimeType" : "application/gzip",
          "lineCount" : 17,
          "fileType" : "csv",
          "md5Checksum" : "1ad61c1190412cdf69931c5cf620e70f"
        }
      ]
      """
    )
  }

  test("Generating a AudioInformationExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      AudioInformationExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "audio-information.csv.gz",
          "sizeBytes" : 121,
          "mimeType" : "application/gzip",
          "lineCount" : 0,
          "fileType" : "csv",
          "md5Checksum" : "53aeb330a59b4e10bb16d57aff29ef3a"
        }
      ]
      """
    )
  }

  test("Generating a ImageInformationExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      ImageInformationExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "image-information.csv.gz",
          "sizeBytes" : 887,
          "mimeType" : "application/gzip",
          "lineCount" : 7,
          "fileType" : "csv",
          "md5Checksum" : "e25a113f5a8fe0e067ce9f3a747fa5ed"
        }
      ]
      """
    )
  }

  test("Generating a PdfInformationExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      PdfInformationExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "pdf-information.csv.gz",
          "sizeBytes" : 121,
          "mimeType" : "application/gzip",
          "lineCount" : 0,
          "fileType" : "csv",
          "md5Checksum" : "53aeb330a59b4e10bb16d57aff29ef3a"
        }
      ]
      """
    )
  }

  test("Generating a PresentationProgramInformationExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      PresentationProgramInformationExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "powerpoint-information.csv.gz",
          "sizeBytes" : 121,
          "mimeType" : "application/gzip",
          "lineCount" : 0,
          "fileType" : "csv",
          "md5Checksum" : "53aeb330a59b4e10bb16d57aff29ef3a"
        }
      ]
      """
    )
  }

  test("Generating a SpreadsheetInformationExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      SpreadsheetInformationExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "spreadsheet-information.csv.gz",
          "sizeBytes" : 121,
          "mimeType" : "application/gzip",
          "lineCount" : 0,
          "fileType" : "csv",
          "md5Checksum" : "53aeb330a59b4e10bb16d57aff29ef3a"
        }
      ]
      """
    )
  }

  test("Generating a VideoInformationExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      VideoInformationExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "video-information.csv.gz",
          "sizeBytes" : 121,
          "mimeType" : "application/gzip",
          "lineCount" : 0,
          "fileType" : "csv",
          "md5Checksum" : "53aeb330a59b4e10bb16d57aff29ef3a"
        }
      ]
      """
    )
  }

  test("Generating a WordProcessorInformationExtraction on the test collection works") {
    val (user, apiKey) = makeArchUser()
    runJobAndVerify(
      user,
      apiKey,
      WordProcessorInformationExtraction,
      Map("type" -> "collection", "collectionId" -> "SPECIAL-test-collection"),
      """
      [
        {
          "filename" : "word-document-information.csv.gz",
          "sizeBytes" : 121,
          "mimeType" : "application/gzip",
          "lineCount" : 0,
          "fileType" : "csv",
          "md5Checksum" : "53aeb330a59b4e10bb16d57aff29ef3a"
        }
      ]
      """
    )
  }

}

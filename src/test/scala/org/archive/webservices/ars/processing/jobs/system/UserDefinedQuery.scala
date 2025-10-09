package test.org.archive.webservices.ars

import scala.concurrent.duration._
import scala.concurrent.Await

import io.circe.syntax._

import org.archive.webservices.ars.io.WebArchiveLoader
import org.archive.webservices.ars.model.collections.inputspecs.InputSpec
import org.archive.webservices.ars.processing.DerivationJobParameters
import org.archive.webservices.ars.processing.jobs.system.UserDefinedQuery.filterQuery
import org.archive.webservices.ars.processing.SparkJobManager
import org.archive.webservices.sparkling.Sparkling.executionContext

class UserDefinedQuerySpec extends UnitSpec {

  def getMatchingSurtUrls(surtPrefixesOR: Seq[String]): Set[String] = {
    val inputSpec = InputSpec.apply(
      "{\"type\": \"collection\", \"collectionId\": \"SPECIAL-test-collection\"}"
    )

    val params = DerivationJobParameters(Map("surtPrefixesOR" -> surtPrefixesOR.asJson))

    Await.result(
      SparkJobManager.context.map { _ =>
        WebArchiveLoader.loadCdx(inputSpec) {
          rdd => filterQuery(rdd, params).map(_.surtUrl)
        }.collect
      },
      30 seconds
    ).toSet
  }

  test("filterQuery SURT 'com,googleapis,fonts)/ ' matches only that one path") {
    val surtUrls = getMatchingSurtUrls(Seq("com,googleapis,fonts)/ "))
    val expectedSurtUrls = Set("com,googleapis,fonts)/")
    surtUrls should be (expectedSurtUrls)
  }

  test("filterQuery SURT 'com,w' matches all wp.com and wordpress.com subdomains") {
    val surtUrlsDomains = getMatchingSurtUrls(Seq("com,w")).map(_.takeWhile(c => c != ')')).toSet
    val expectedSurtUrlDomains = Set(
      "com,wordpress",
      "com,wordpress,files,netpreserveblog",
      "com,wordpress,netpreserveblog",
      "com,wordpress,public-api",
      "com,wp,pixel",
      "com,wp,s0",
      "com,wp,s1",
      "com,wp,s2",
      "com,wp,stats",
      "com,wp,widgets",
    )
    surtUrlsDomains should be (expectedSurtUrlDomains)
  }

  test("filterQuery SURT 'com,wp' matches all wp.com subdomains") {
    val surtUrlsDomains = getMatchingSurtUrls(Seq("com,wp")).map(_.takeWhile(c => c != ')')).toSet
    val expectedSurtUrlDomains = Set(
      "com,wp,pixel",
      "com,wp,s0",
      "com,wp,s1",
      "com,wp,s2",
      "com,wp,stats",
      "com,wp,widgets",
    )
    surtUrlsDomains should be (expectedSurtUrlDomains)
  }

  test("filterQuery SURT 'com,googleapis,fonts)' matches all paths") {
    val surtUrls = getMatchingSurtUrls(Seq("com,googleapis,fonts)"))
    val expectedSurtUrls = Set(
      "com,googleapis,fonts)",
      "com,googleapis,fonts)/",
      "com,googleapis,fonts)/css?family=libre%20baskerville:r|open%20sans:r,i,b,bi&subset=latin,latin-ext,latin,latin-ext",
      "com,googleapis,fonts)/css?family=roboto:400,700,700italic,400italic|playfair%20display:400,400italic,700italic,700&subset=latin,latin-ext",
      "com,googleapis,fonts)/robots.txt",
    )
    surtUrls should be (expectedSurtUrls)
  }

  test("filterQuery SURT 'com,googleapis,fonts)/' matches all paths with leading slash") {
    val surtUrls = getMatchingSurtUrls(Seq("com,googleapis,fonts)/"))
    val expectedSurtUrls = Set(
      "com,googleapis,fonts)/",
      "com,googleapis,fonts)/css?family=libre%20baskerville:r|open%20sans:r,i,b,bi&subset=latin,latin-ext,latin,latin-ext",
      "com,googleapis,fonts)/css?family=roboto:400,700,700italic,400italic|playfair%20display:400,400italic,700italic,700&subset=latin,latin-ext",
      "com,googleapis,fonts)/robots.txt",
    )
    surtUrls should be (expectedSurtUrls)
  }

}

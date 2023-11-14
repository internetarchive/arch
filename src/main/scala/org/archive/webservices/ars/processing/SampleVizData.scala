package org.archive.webservices.ars.processing

import io.circe._
import io.circe.generic.semiauto._


case class SampleVizData(nodes: Seq[(String, String)], edges: Option[Seq[(String, String)]] = None)

object SampleVizData {
  implicit val sampleVizDataEncoder: Encoder[SampleVizData] = deriveEncoder
}

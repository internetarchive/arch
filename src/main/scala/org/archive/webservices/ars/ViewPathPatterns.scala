package org.archive.webservices.ars

import org.scalatra.SinatraRouteMatcher

import org.archive.webservices.ars.model.ArchConf

object ViewPathPatterns {
  val Collection = "/collections/:collection_id"
  val Collections = "/collections"
  val CustomCollectionBuilder = "/collections/custom-collection-builder"
  val Dataset = "/datasets/:dataset_id"
  val DatasetExplorer = "/datasets/explore"
  val Datasets = "/datasets"
  val GenerateDataset = "/datasets/generate"
  val Home = "/?"
  val Login = "/login"

  def reverse(pattern: String, paramsMap: Map[String, String] = Map.empty): String = {
    (new SinatraRouteMatcher(pattern)).reverse(paramsMap, List()).stripSuffix("?")
  }

  def reverseAbs(pattern: String, paramsMap: Map[String, String] = Map.empty): String = {
    ArchConf.baseUrl + reverse(pattern, paramsMap)
  }
}

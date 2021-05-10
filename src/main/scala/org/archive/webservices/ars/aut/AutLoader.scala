package org.archive.webservices.ars.aut

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{
  DataType,
  IntegerType,
  LongType,
  StringType,
  StructField,
  StructType
}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}

object AutLoader {
  lazy val session: SparkSession = SparkSession.builder.getOrCreate

  private def df(rows: RDD[Row], fields: (String, DataType)*): DataFrame = {
    val schema = new StructType(
      fields.map { case (n, t) => StructField(n, t, nullable = true) }.toArray)
    session.createDataFrame(rows, schema)
  }

  def save(data: Dataset[Row], path: String): Unit = {
    data.write
      .option("timestampFormat", "yyyy/MM/dd HH:mm:ss ZZ")
      .format("csv")
      .save(path)
  }

  def saveAndLoad(data: Dataset[Row], path: String): Dataset[Row] = {
    save(data, path)
    session.read
      .option("timestampFormat", "yyyy/MM/dd HH:mm:ss ZZ")
      .format("csv")
      .schema(data.schema)
      .load(path)
  }

  def webpages(rows: RDD[Row]): DataFrame = {
    df(
      rows,
      ("crawl_date", StringType),
      ("domain", StringType),
      ("url", StringType),
      ("mime_type_web_server", StringType),
      ("mime_type_tika", StringType),
      ("language", StringType),
      ("content", StringType))
  }

  def domainFrequency(rows: RDD[Row]): DataFrame = {
    df(rows, ("domain", StringType), ("count", LongType))
  }

  def domainGraph(rows: RDD[Row]): DataFrame = {
    df(
      rows,
      ("crawl_date", StringType),
      ("src_domain", StringType),
      ("dest_domain", StringType),
      ("count", LongType))
  }

  def webGraph(rows: RDD[Row]): DataFrame = {
    df(
      rows,
      ("crawl_date", StringType),
      ("src", StringType),
      ("dest", StringType),
      ("anchor", StringType))
  }

  def imageGraph(rows: RDD[Row]): DataFrame = {
    df(
      rows,
      ("crawl_date", StringType),
      ("src", StringType),
      ("image_url", StringType),
      ("alt_text", StringType))
  }

  def images(rows: RDD[Row]): DataFrame = {
    df(
      rows,
      ("crawl_date", StringType),
      ("url", StringType),
      ("filename", StringType),
      ("extension", StringType),
      ("mime_type_web_server", StringType),
      ("mime_type_tika", StringType),
      ("width", IntegerType),
      ("height", IntegerType),
      ("md5", StringType),
      ("sha1", StringType))
  }

  def binaryInformation(rows: RDD[Row]): DataFrame = {
    df(
      rows,
      ("crawl_date", StringType),
      ("url", StringType),
      ("filename", StringType),
      ("extension", StringType),
      ("mime_type_web_server", StringType),
      ("mime_type_tika", StringType),
      ("md5", StringType),
      ("sha1", StringType))
  }
}

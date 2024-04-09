package org.archive.webservices.ars.processing.jobs.system

import org.archive.webservices.ars.io.WebArchiveLoader
import org.archive.webservices.ars.model.collections.CustomCollectionSpecifics
import org.archive.webservices.ars.model.{ArchJobCategories, ArchJobCategory, DerivativeOutput}
import org.archive.webservices.ars.processing._
import org.archive.webservices.ars.processing.jobs.ArsLgaGeneration.id
import org.archive.webservices.sparkling.Sparkling
import org.archive.webservices.sparkling.Sparkling.executionContext
import org.archive.webservices.sparkling.cdx.CdxLoader
import org.archive.webservices.sparkling.io._
import org.archive.webservices.sparkling.logging.LogContext
import org.archive.webservices.sparkling.util.{RddUtil, SurtUtil, Time14Util}

import scala.concurrent.Future

object UserDefinedQuery extends SparkJob {
  implicit val logContext: LogContext = LogContext(this)

  val name = "User-Defined Query"
  val uuid = "018950a1-6773-79f3-8eb2-fba4356e23b9"
  val category: ArchJobCategory = ArchJobCategories.System
  def description = "Job to run a user-defined query (internal system job)"

  val relativeOutPath: String = s"/$id"

  private def checkFieldOperators[T: io.circe.Decoder](
      params: DerivationJobParameters,
      fields: Seq[String])(check: T => Boolean): Boolean =
    fields.forall(checkFieldOperators(params, _)(check))

  private def checkFieldOperators[T: io.circe.Decoder](
      params: DerivationJobParameters,
      field: String)(check: T => Boolean): Boolean = {
    val or = params.values
      .get(field + "OR")
      .map(_.hcursor)
      .toSeq
      .flatMap(c => c.values.getOrElse(c.focus.toSeq).flatMap(_.as[T].toOption)) ++ {
      params.get[T](field).toSeq
    }
    val not = params.values
      .get(field + "NOT")
      .map(_.hcursor)
      .toSeq
      .flatMap(c => c.values.getOrElse(c.focus.toSeq).flatMap(_.as[T].toOption))
    (or.isEmpty || or.exists(check)) && (not.isEmpty || !not.exists(check))
  }

  private def validateFields[T: io.circe.Decoder](
      params: DerivationJobParameters,
      fields: Seq[String],
      addOperators: Boolean = false)(validate: T => Option[String]): Option[String] = {
    fields.toIterator
      .map { field =>
        validateField(params, field, addOperators = addOperators)(validate)
      }
      .find(_.isDefined)
      .flatten
  }

  private def validateField[T: io.circe.Decoder](
      params: DerivationJobParameters,
      field: String,
      addOperators: Boolean = false)(validate: T => Option[String]): Option[String] = {
    if (addOperators)
      validateFields(params, Seq(field, field + "OR", field + "NOT"), addOperators = false)(
        validate)
    else {
      val values = params.values
        .get(field)
        .map(_.hcursor)
        .toSeq
        .flatMap(c => c.values.getOrElse(c.focus.toSeq))
      values.toIterator.find(v => v.asString.getOrElse(v.toString).trim.isEmpty) match {
        case Some(_) => Some("Invalid empty value for field " + field + ".")
        case None =>
          val parsed = values.map(v => (v, v.as[T].toOption))
          parsed.find(_._2.isEmpty) match {
            case Some((v, _)) => Some("Invalid value for field " + field + ": " + v.noSpaces)
            case None => parsed.toIterator.flatMap(_._2).map(validate).find(_.isDefined).flatten
          }
      }
    }
  }

  override def validateParams(conf: DerivationJobConf): Option[String] = {
    super.validateParams(conf).orElse {
      validateFields[String](
        conf.params,
        Seq("surtPrefix", "surtPrefixes"),
        addOperators = true) { v =>
        if (v.contains(")")) SurtUtil.validateHost(v) match {
          case Some(_) => None
          case None => Some("Invalid host in " + v)
        }
        else if (v.contains(".") || v.contains(" ")) Some("Invalid SURT prefix: " + v)
        else None
      }.orElse {
        validateFields[String](conf.params, Seq("timestampFrom", "timestampTo")) { v =>
          Time14Util.validate(v) match {
            case Some(_) => None
            case None => Some("Invalid timestamp: " + v)
          }
        }
      }
    }
  }

  def run(conf: DerivationJobConf): Future[Boolean] = {
    SparkJobManager.context.map { sc =>
      SparkJobManager.initThread(sc, UserDefinedQuery, conf)
      WebArchiveLoader.loadCdx(conf.inputSpec) { rdd =>
        val paramsBc = sc.broadcast(conf.params)
        val filtered = rdd
          .mapPartitions { partition =>
            val params = paramsBc.value
            partition.filter { cdx =>
              {
                checkFieldOperators[String](params, Seq("surtPrefix", "surtPrefixes"))(
                  cdx.surtUrl.startsWith)
              } && {
                params.get[String]("timestampFrom").forall(cdx.timestamp >= _)
              } && {
                params
                  .get[String]("timestampTo")
                  .forall(t => cdx.timestamp <= t || cdx.timestamp.startsWith(t))
              } && {
                checkFieldOperators[Int](params, "status")(cdx.status == _)
              } && {
                checkFieldOperators[Int](params, Seq("statusPrefix", "statusPrefixes"))(s =>
                  cdx.status.toString.startsWith(s.toString))
              } && {
                checkFieldOperators[String](params, Seq("mime", "mimes"))(cdx.mime == _)
              } && {
                checkFieldOperators[String](params, Seq("mimePrefix", "mimePrefixes"))(
                  cdx.mime.startsWith)
              }
            }
          }
        val cdxPath = conf.outputPath + "/" + CustomCollectionSpecifics.CdxDir
        RddUtil.saveAsTextFile(
          filtered.map(_.toCdxString),
          cdxPath,
          skipIfExists = true,
          checkPerFile = true,
          skipEmpty = true)
        if (HdfsIO.exists(cdxPath + "/" + Sparkling.CompleteFlagFile)) {
          val size = CdxLoader.load(cdxPath + "/*.cdx.gz").map(_.compressedSize).fold(0L)(_ + _)
          val info = conf.params.set("size" -> size)
          val infoPath = conf.outputPath + "/" + CustomCollectionSpecifics.InfoFile
          HdfsIO.writeLines(infoPath, Seq(info.toJson.spaces4))
          HdfsIO.exists(infoPath)
        } else false
      }
    }
  }

  override def history(conf: DerivationJobConf): DerivationJobInstance = {
    val instance = super.history(conf)
    val started = HdfsIO.exists(conf.outputPath + relativeOutPath)
    if (started) {
      val completed = HdfsIO.exists(conf.outputPath + "/" + CustomCollectionSpecifics.InfoFile)
      instance.state = if (completed) ProcessingState.Finished else ProcessingState.Failed
    }
    instance
  }

  override def outFiles(conf: DerivationJobConf): Iterator[DerivativeOutput] = Iterator.empty

  override val templateName: Option[String] = None

  override def reset(conf: DerivationJobConf): Unit = HdfsIO.delete(conf.outputPath)

  override val finishedNotificationTemplate: Option[String] = Some("udq-finished")
}

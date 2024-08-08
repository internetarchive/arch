package org.archive.webservices.ars.util

import io.circe.Json
import io.circe.syntax.EncoderOps
import org.archive.webservices.sparkling.io.HdfsIO
import org.scalatra.{ActionResult, Ok}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LazyCache {
  val writingSuffix = "_writing"

  private var lazyFuture = Future(true)
  private var processing = Map.empty[String, Future[Boolean]]

  def getOrCache[A](
      cacheFile: String)(parse: String => Option[A], write: String => Unit): Future[A] =
    synchronized {
      val parsed = getIfCached(cacheFile)(parse)
      if (parsed.isDefined) return Future(parsed.get)
      processing
        .getOrElse(
          cacheFile, {
            lazyFuture = lazyFuture.map(_ => {
              val tmpFilePath = cacheFile + writingSuffix
              write(tmpFilePath)
              HdfsIO.rename(tmpFilePath, cacheFile)
              true
            })
            processing += cacheFile -> lazyFuture
            lazyFuture
          })
        .map { _ =>
          processing -= cacheFile
          parse(cacheFile).get
        }
    }

  def getIfCached[A](cacheFile: String)(parse: String => Option[A]): Option[A] = {
    if (HdfsIO.exists(cacheFile)) parse(cacheFile) else None
  }

  def lazyJsonResponse[A](
      cached: Option[Future[A]])(orElse: => A, json: A => Json): ActionResult = {
    lazyJsonResponse[A, A](cached, identity, orElse, json)
  }

  def lazyJsonResponse[A, B](
      cached: Option[Future[A]],
      map: A => B,
      orElse: => B,
      json: B => Json): ActionResult = {
    cached match {
      case Some(future) =>
        if (future.isCompleted) {
          Ok(
            json(map(future.value.flatMap(_.toOption).get)).spaces4,
            Map("Content-Type" -> "application/json"))
        } else {
          Ok(Map("lazy" -> true).asJson.spaces4, Map("Content-Type" -> "application/json"))
        }
      case None =>
        Ok(json(orElse).spaces4, Map("Content-Type" -> "application/json"))
    }
  }

  def lazyProcess[A](cached: Option[Future[A]], orElse: => A)(process: A => Unit): Unit = {
    lazyProcess[A, A](cached, identity, orElse)(process)
  }

  def lazyProcess[A, B](cached: Option[Future[A]], map: A => B, orElse: => B)(
      process: B => Unit): Unit = {
    cached match {
      case Some(future) =>
        future.onComplete(v => process(map(v.get)))
      case None =>
        process(orElse)
    }
  }
}

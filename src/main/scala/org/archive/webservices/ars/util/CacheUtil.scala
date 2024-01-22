package org.archive.webservices.ars.util

import org.scalatra.ActionResult
import org.scalatra.guavaCache.GuavaCache

import javax.servlet.http.HttpServletRequest
import scala.concurrent.duration._

object CacheUtil {
  val Charset: String = "UTF-8"

  val RequestCacheDuration: Duration = 10.minutes

  def cache[R](key: String, enabled: Boolean = true, ttl: Option[Duration] = None)(
      value: => R): R =
    if (enabled) {
      GuavaCache.get[R](key) match {
        case Some(cached) => cached
        case None =>
          val v = value
          GuavaCache.put(key, v, ttl)
          v
      }
    } else value

  def put[R](key: String, value: R, enabled: Boolean = true, ttl: Option[Duration] = None): R = {
    GuavaCache.put(key, value, ttl)
    value
  }

  def get[R](key: String): Option[R] = GuavaCache.get[R](key)

  def cacheRequest(
      request: HttpServletRequest,
      enabled: Boolean = true,
      subjects: Set[Any] = Set.empty)(value: => ActionResult): ActionResult =
    if (enabled) {
      val key = "request#" + request.getRequestURI + "?" + request.getQueryString
      Iterator
        .continually {
          GuavaCache.get[Option[ActionResult]](key) match {
            case Some(cached) =>
              if (cached.isEmpty) Thread.sleep(1000)
              cached
            case None =>
              GuavaCache.put(key, None, None)
              try {
                val result = value
                if (result.status.code == 200)
                  GuavaCache.put(key, Some(result), Some(RequestCacheDuration))
                else GuavaCache.remove(key)
                Some(result)
              } catch {
                case e: Exception =>
                  GuavaCache.remove(key)
                  throw e
              }
          }
        }
        .flatten
        .next()
    } else value
}

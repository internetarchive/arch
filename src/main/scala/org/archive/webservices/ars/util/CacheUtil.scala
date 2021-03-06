package org.archive.webservices.ars.util

import javax.servlet.http.HttpServletRequest
import org.scalatra.ActionResult
import org.scalatra.guavaCache.GuavaCache

import scala.concurrent.duration._

object CacheUtil {
  val Charset: String = "UTF-8"

  val RequestCacheDuration: Duration = 10.minutes

  def cache[R](key: String, enabled: Boolean = true)(value: => R): R =
    if (enabled) {
      GuavaCache.get[R](key) match {
        case Some(cached) => cached
        case None =>
          val v = value
          GuavaCache.put(key, v, None)
          v
      }
    } else value

  def cacheRequest(request: HttpServletRequest, enabled: Boolean = true)(
      value: => ActionResult): ActionResult =
    if (enabled) {
      Iterator
        .continually {
          val key = "request#" + request.getRequestURI + "?" + request.getQueryString
          GuavaCache.get[Option[ActionResult]](key) match {
            case Some(cached) =>
              if (cached.isEmpty) Thread.sleep(1000)
              cached
            case None =>
              GuavaCache.put(key, None, None)
              val result = value
              if (result.status.code == 200)
                GuavaCache.put(key, Some(result), Some(RequestCacheDuration))
              else GuavaCache.remove(key)
              Some(result)
          }
        }
        .flatten
        .next()
    } else value
}

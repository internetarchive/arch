package org.archive.webservices.ars.util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object FuturesUtil {
  // https://stackoverflow.com/a/29344937
  private def lift[T](futures: Seq[Future[T]]) =
    futures.map(_.map { Success(_) }.recover { case t => Failure(t) })

  def waitAll[T](futures: Seq[Future[T]]) =
    Future.sequence(
      lift(futures)
    ) // having neutralized exception completions through the lifting, .sequence can now be used
}

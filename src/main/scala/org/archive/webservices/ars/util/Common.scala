package org.archive.webservices.ars.util

object Common {
  def tryOrElse[R](orElse: => R)(action: => R): R = {
    try {
      action
    } catch {
      case e: Exception =>
        e.printStackTrace()
        orElse
    }
  }

  def retryWhile(cond: => Boolean, sleepMs: Int, maxTimes: Int, sleepInc: Int => Int): Boolean = {
    var sleep = sleepMs
    var times = 1
    var result = cond
    while (result && times < maxTimes) {
      Thread.sleep(sleep)
      sleep = sleepInc(sleep)
      times += 1
      result = cond
    }
    !result
  }
}

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
}

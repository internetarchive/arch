package org.archive.webservices.ars.util

import java.time.Instant

import org.archive.webservices.sparkling.util.StringUtil

object FormatUtil {
  def formatBytes(bytes: Long): String = {
    val units = Seq("B", "KB", "MB", "GB", "TB", "PB")
    if (bytes < 0) "0 " + units.head
    else {
      var unitIdx = 0
      var b = bytes.toDouble
      while (b > 1024 && unitIdx < units.length - 1) {
        unitIdx += 1
        b = b / 1024
      }
      StringUtil.formatNumber(b, 1) + " " + units(unitIdx)
    }
  }

  def instantTimeString(instant: Instant): String =
    instant.toString.stripSuffix("Z").replace("T", " ")
}

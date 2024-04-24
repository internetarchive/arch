package org.archive.webservices.ars.model.collections.inputspecs

import java.io.FileNotFoundException

trait LongestPrefixProbing {
  protected def locateLongestPrefixPath(filename: String): String = {
    var remaining = filename
    var prefix = ""
    var next = nextPrefixes(prefix)
    while (next.nonEmpty) {
      val keys =
        next.map(p => (p, p.stripPrefix(prefix).stripSuffix("/"))).filter(_._2.nonEmpty)
      val longest = keys
        .filter { case (_, k) =>
          remaining.startsWith(k)
        }
        .toSeq
        .sortBy(-_._2.length)
        .headOption
        .orElse {
          keys
            .filter { case (_, k) =>
              filename.startsWith(k)
            }
            .toSeq
            .sortBy(-_._2.length)
            .headOption
        }
      if (longest.isEmpty) throw new FileNotFoundException(filename + s" ($prefix)")
      val (p, k) = longest.get
      if (k == filename) return prefix.stripSuffix("/")
      if (remaining.startsWith(k)) remaining = remaining.stripPrefix(k)
      prefix = p
      next = nextPrefixes(prefix)
    }
    throw new FileNotFoundException(filename + s" ($prefix)")
  }

  protected def nextPrefixes(prefix: String): Set[String]
}

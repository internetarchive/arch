package org.archive.webservices.ars.io

import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil}
import org.archive.webservices.sparkling.util.Common

import java.io.InputStream

class CacheBackedInputStream(
    source: => InputStream,
    cachePath: Option[String] = None,
    direct: => Option[InputStream] = None,
    rewriteCache: Boolean = false
) extends MultiSourceInputStream {
  private var readDirect = false
  private var readCache = rewriteCache
  private var rewritten = false
  private var readSource = false

  override def nextSource(pos: Long): Option[InputStream] = {
    if (!readDirect) try {
      for (in <- direct) {
        IOUtil.skip(in, pos)
        return Some(in)
      }
    } finally {
      readDirect = true
    }
    if (!readCache || !rewritten) try {
      for (c <- cachePath) {
        // if cache was read before, and still here, then rewrite it
        if (!HdfsIO.exists(c) || readCache) try {
          IOHelper.syncHdfs(c + "_caching") { waited =>
            val exists = HdfsIO.exists(c)
            if (exists) HdfsIO.delete(c)
            if (!exists || !waited) {
              val tmp = s"$c._writing"
              val out = HdfsIO.out(tmp, compress = false, overwrite = true, useWriter = false)
              try {
                val s = source
                try {
                  IOUtil.copy(s, out)
                } finally {
                  s.close()
                }
              } finally {
                out.close()
              }
              HdfsIO.rename(tmp, c)
              while (HdfsIO.exists(tmp) || !HdfsIO.exists(c)) Thread.`yield`()
            }
          }
        } finally {
          rewritten = true
        }
        return Some(HdfsIO.open(c, offset = pos, decompress = false))
      }
    } finally {
      readCache = true
    }
    if (!readSource) try {
      val s = source
      IOUtil.skip(s, pos)
      return Some(s)
    } finally {
      readSource = true
    }
    None
  }
}

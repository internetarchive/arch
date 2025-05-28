package org.archive.webservices.ars.io

import org.archive.webservices.sparkling.io.{HdfsIO, IOUtil}

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
    if (!readDirect) {
      readDirect = true
      for (in <- direct) {
        IOUtil.skip(in, pos)
        return Some(in)
      }
    }
    if (!readCache || !rewritten) {
      readCache = true
      for (c <- cachePath) {
        if (!HdfsIO.exists(c) || readCache) {
          rewritten = true
          IOHelper.syncHdfs(c + "_caching") { waited =>
            if (!HdfsIO.exists(c) || !waited) {
              val out = HdfsIO.out(c, compress = false, overwrite = true, useWriter = false)
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
            }
          }
        }
        return Some(HdfsIO.open(c, offset = pos, decompress = false))
      }
    }
    if (!readSource) {
      readSource = true
      val s = source
      IOUtil.skip(s, pos)
      return Some(s)
    }
    None
  }
}

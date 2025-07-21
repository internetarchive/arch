package org.archive.webservices.ars.io

import java.io.InputStream
import scala.annotation.tailrec
import scala.util.Try

abstract class MultiSourceInputStream extends InputStream {
  private var eof = false
  private var closed = false

  private var pos = 0L

  private var _in: Option[InputStream] = None

  private def nextIn(e: Option[Exception] = None): InputStream = {
    try {
      _in = nextSource(pos)
    } catch {
      case e: Exception =>
        return nextIn(Some(e))
    }
    _in match {
      case Some(in) => in
      case None => throw e.getOrElse(new RuntimeException("No more sources."))
    }
  }

  private def in: InputStream = _in.getOrElse(nextIn())

  def nextSource(pos: Long): Option[InputStream]

  @tailrec
  private def tryOrNext[R](action: => R, ifEof: R, reading: Boolean = false): R = {
    if (eof) ifEof else {
      try {
        val r = action
        if (reading && r == ifEof) eof = true
        r
      } catch {
        case e: Exception =>
          if (closed) throw e
          else {
            Try(in.close())
            nextIn(Some(e))
            tryOrNext(action, ifEof, reading)
          }
      }
    }
  }

  private def readOrNext(action: => Int): Int = {
    val read = tryOrNext(action, -1, reading = true)
    if (read >= 0) pos += read
    read
  }

  override def read(): Int = readOrNext(in.read())
  override def read(b: Array[Byte]): Int = readOrNext(in.read(b))
  override def read(b: Array[Byte], off: Int, len: Int): Int = readOrNext(in.read(b, off, len))
  override def skip(n: Long): Long = tryOrNext(in.skip(n), 0)
  override def available(): Int = tryOrNext(in.available(), 0)
  override def close(): Unit = {
    Try(in.close())
    closed = true
  }
  override def markSupported(): Boolean = false
}

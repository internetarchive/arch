package org.archive.webservices.ars.io

import java.io.{ByteArrayOutputStream, EOFException, InputStream}

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream, BZip2CompressorOutputStream}
import org.archive.helge.sparkling.io.ByteArray

object MemoryCompressor {
  val BufferSize = 1024

  def compress(in: InputStream): ByteArray = {
    val array = new ByteArray
    val buffer = new Array[Byte](BufferSize)
    val out = new ByteArrayOutputStream()
    val compressor = new BZip2CompressorOutputStream(out)
    try {
      var read = in.read(buffer)
      while (read != -1) {
        if (read > 0) compressor.write(buffer, 0, read)
        if (out.size > BufferSize) {
          array.append(out.toByteArray)
          out.reset()
        }
        read = in.read(buffer)
      }
    } catch {
      case _: EOFException => // ignore EOF / break loop
    }
    compressor.close()
    array.append(out.toByteArray)
    array
  }

  def decompress(array: ByteArray): InputStream = new BZip2CompressorInputStream(array.toInputStream)
}

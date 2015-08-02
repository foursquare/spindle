// Copyright 2015 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.common.thrift.bson

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.Charset

object ByteStringBuilder {
  val UTF8_CHARSET = Charset.forName("UTF-8")
  val MaxSize = 16 * 1024 * 1024
}

/**
 * Growable buffer for building strings. Not thread safe
 * reset() must be called before each re-use
 */
class ByteStringBuilder(initialSize: Int) {
  private var bytes = new Array[Byte](initialSize)
  private var length = 0
  
  private def ensureGrowth(size: Int) {
    val newRequestedSize = length + size
    if (newRequestedSize > bytes.length) {
      if (newRequestedSize > ByteStringBuilder.MaxSize) {
        throw new RuntimeException(s"Attempting to grow string builder past maximum size $newRequestedSize")
      }
      val newLength = math.min(newRequestedSize * 1.25, ByteStringBuilder.MaxSize).toInt
      val newBytes = new Array[Byte](newLength)
      System.arraycopy(bytes, 0, newBytes, 0, length)
      bytes = newBytes
    }
  }

  def append(b: Byte) {
    ensureGrowth(1)
    bytes(length) = b
    length += 1
  }

  def reset() {
    length = 0
  }

  def build(): String = {
    new String(bytes, 0, length, ByteStringBuilder.UTF8_CHARSET)
  }

  /**
   * copy bytes from InputStream into this builder
   */
  def read(is: InputStream, readLength: Int) {
    ensureGrowth(readLength)
    is.read(bytes, length, readLength)
    length += readLength
  }
}
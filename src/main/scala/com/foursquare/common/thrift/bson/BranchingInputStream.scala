// Copyright 2015 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.common.thrift.bson

import java.io.{ByteArrayInputStream, InputStream}

/**
 * version of ByteArrayInputStream that allows streams to be created
 * from backing array without doing any copying
 */
class BranchingInputStream(
  bytes: Array[Byte],
  offset: Int,
  length: Int
) extends ByteArrayInputStream(bytes, offset, length) {
  def branch(length: Int): BranchingInputStream = {
    val newPos = this.pos
    this.pos += length
    new BranchingInputStream(this.buf, newPos, length)
  }
}
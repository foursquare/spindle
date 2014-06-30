// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

import org.apache.thrift.protocol.TProtocol

trait UntypedRecord {
  def meta: UntypedMetaRecord

  def read(iprot: TProtocol): Unit
  def write(oprot: TProtocol): Unit
}

trait Record[R <: Record[R]] extends UntypedRecord with scala.Ordered[R] { self: R =>
  def meta: MetaRecord[R]

  def mergeCopy(that: R): R
}

trait MutableRecord[R <: Record[R]] { self: R =>
  def merge(that: R): Unit
}

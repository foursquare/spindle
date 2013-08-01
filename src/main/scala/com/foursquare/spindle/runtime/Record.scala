// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

import org.apache.thrift.protocol.TProtocol

trait UntypedRecord {
  def meta: UntypedMetaRecord

  def read(iprot: TProtocol): Unit
  def write(oprot: TProtocol): Unit
}

trait Record[R <: Record[R]] extends UntypedRecord { self: R =>
  type MetaT <: MetaRecord[R]
  def meta: MetaT
}

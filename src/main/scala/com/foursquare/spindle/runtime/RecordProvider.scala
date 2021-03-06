// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

import org.apache.thrift.{TBase, TFieldIdEnum}

trait RecordProvider[R <: TBase[_ <: TBase[_, _], _ <: TFieldIdEnum]] {
  def createRecord: R
}

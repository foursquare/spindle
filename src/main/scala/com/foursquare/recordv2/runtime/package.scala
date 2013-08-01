// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare

package object spindle {
  trait Tagged[U]
  type Id[T, U] = T with Tagged[U]
}

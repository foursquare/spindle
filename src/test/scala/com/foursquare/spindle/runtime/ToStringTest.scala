// Copyright 2014 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.runtime.test

import org.bson.types.ObjectId
import org.junit.Assert.assertEquals
import org.junit.Test
import com.foursquare.spindle.test.gen.MapsWithNonStringKeys


class ToStringTest {

  @Test
  def testToString() {
    val struct = MapsWithNonStringKeys.newBuilder
      .foo(Map(123 -> "A", 456 -> "B"))
      .bar(Map(new ObjectId("000102030405060708090a0b") -> 77,
               new ObjectId("0c0d0e0f1011121314151617") -> 42))
      .result()

    val expected =
      """{ "foo" : { "123" : "A", "456" : "B" }, "bar" : { "ObjectId(\"000102030405060708090a0b\")" : 77, "ObjectId(\"0c0d0e0f1011121314151617\")" : 42 } }"""
    val str = struct.toString()
    assertEquals(expected, str)
  }
}

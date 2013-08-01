// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.test

import com.foursquare.spindle.BitFieldHelpers
import org.junit.Test
import org.specs.SpecsMatchers

class BitFieldHelpersTest extends SpecsMatchers {
  @Test
  def testLongFlags() {
    // Flag at place 1 is set true, flag at place 0 is set false, all others unset
    val sanityFlags = (3L << 32) | 2
    BitFieldHelpers.getLongIsSet(sanityFlags, 2) must_== false

    BitFieldHelpers.getLongIsSet(sanityFlags, 1) must_== true
    BitFieldHelpers.getLongValue(sanityFlags, 1) must_== true

    BitFieldHelpers.getLongIsSet(sanityFlags, 0) must_== true
    BitFieldHelpers.getLongValue(sanityFlags, 0) must_== false
  }
}

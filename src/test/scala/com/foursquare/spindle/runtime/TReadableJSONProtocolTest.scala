// Copyright 2014 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.runtime.test

import com.foursquare.common.thrift.json.TReadableJSONProtocol
import com.foursquare.spindle.{Record, MetaRecord}
import com.foursquare.spindle.test.gen.TestStruct
import org.apache.thrift.TBase
import org.apache.thrift.transport.TMemoryInputTransport
import org.junit.Assert.assertEquals
import org.junit.Test

class TReadableJSONProtocolTest {

  @Test
  def testNullValues {
    val t1 = parseJson("""{"aString":null}""", TestStruct)
    assertEquals(None, t1.aStringOption)
  }

  def parseJson[R <: Record[R] with TBase[R, _]](s: String, recMeta: MetaRecord[R]): R = {
    val buf = s.getBytes("UTF-8")
    val trans = new TMemoryInputTransport(buf)
    val iprot = new TReadableJSONProtocol(trans, null)
    val rec = recMeta.createRawRecord
    rec.read(iprot)
    rec
  }
}
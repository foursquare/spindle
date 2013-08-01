// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

import com.foursquare.spindle.test.gen.{LongNameInnerStruct, LongNameTestEnum, LongNameTestStruct}
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import org.apache.thrift.transport.TIOStreamTransport
import org.junit.Assert.{assertEquals, assertFalse, assertTrue}
import org.junit.Test

class LongNameRecordProxyText {
  @Test
  def test = {
    val record = LongNameTestStruct.newBuilder
            .aBool(true)
            .aByte(120.toByte)
            .anI16(30000.toShort)
            .anI32(7654321)
            .anI64(987654321L)
            .aDouble(0.57)
            .aString("hello, world")
            .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
            .aStruct(LongNameInnerStruct("hi", 5))
            .aSet(Set("foo", "bar", "baz"))
            .aList(List(4, 8, 15, 16, 23, 42))
            .aMap(Map("uno" -> LongNameInnerStruct("one", 1), "dos" -> LongNameInnerStruct("two", 2)))
            .anEnum(LongNameTestEnum.One)
            .result()
    val proxy = new LongNameRecordProxy(record)

    val protocol = new com.foursquare.common.thrift.json.TReadableJSONProtocol.Factory()
    val baos = new ByteArrayOutputStream
    val transport = new TIOStreamTransport(baos)
    proxy.write(protocol.getProtocol(transport))
    val bytes = baos.toByteArray

    val expected = """{"aBool":true,"aByte":120,"anI16":30000,"anI32":7654321,"anI64":987654321,"aDouble":0.57,"aString":"hello, world","aBinary":"AQIDBAU=","aStruct":{"aString":"hi","anInt":5},"aSet":["foo","bar","baz"],"aList":[4,8,15,16,23,42],"aMap":["uno",{"aString":"one","anInt":1},"dos",{"aString":"two","anInt":2}],"anEnum":"One"}"""
    assertEquals(new String(bytes, "UTF-8"), expected)
  }
}
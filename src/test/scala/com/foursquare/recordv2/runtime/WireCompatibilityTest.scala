// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.test

import com.foursquare.common.thrift.bson.TBSONProtocol
import com.foursquare.spindle.MetaRecord
import com.foursquare.spindle.test.gen.{InnerStruct, TestStruct, TestStructNoBinary, TestStructNoBool,
    TestStructNoByte, TestStructNoDouble, TestStructNoI16, TestStructNoI32, TestStructNoI64, TestStructNoList,
    TestStructNoMap, TestStructNoSet, TestStructNoString, TestStructNoStruct}
import java.nio.ByteBuffer
import org.apache.thrift.TBase
import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol, TProtocolFactory}
import org.apache.thrift.transport.{TMemoryBuffer, TTransport}
import org.junit.Assert.assertEquals
import org.junit.Test


class WireCompatibilityTest {

  @Test
  def testCompatibilityWithBinaryProtocol() {
    doTestSkipUnknownStructField(new TBinaryProtocol.Factory(), new TBinaryProtocol.Factory())
  }

  @Test
  def testCompatibilityWithCompactProtocol() {
    doTestSkipUnknownStructField(new TCompactProtocol.Factory(), new TCompactProtocol.Factory())
  }

  @Test
  def testCompatibilityWithBSONProtocol() {
    doTestSkipUnknownStructField(new TBSONProtocol.WriterFactory(), new TBSONProtocol.ReaderFactory())
  }

  @Test
  def testCompatibilityWithJSONProtocol() {
    // TODO(benjy): JReadableJSONProtocol doesn't yet support maps properly.
  //  doTestSkipUnknownStructField(new TReadableJSONProtocol.Factory(), new TReadableJSONProtocol.Factory())
  }

  private def doTestSkipUnknownStructField(writerFactory: TProtocolFactory, readerFactory: TProtocolFactory) {
    val newObj = TestStruct.newBuilder
            .aBool(true)
            .aByte(120.toByte)
            .anI16(30000.toShort)
            .anI32(7654321)
            .anI64(987654321L)
            .aDouble(0.57)
            .aString("hello, world")
            .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
            .aStruct(InnerStruct("hi", 5))
            .aSet(Set("foo", "bar", "baz"))
            .aList(List(4, 8, 15, 16, 23, 42))
            .aMap(Map("uno" -> InnerStruct("one", 1), "dos" -> InnerStruct("two", 2)))
            .result()

    testReadingUnrecognizedField(TestStructNoBool, _.aBoolUnset())
    testReadingUnrecognizedField(TestStructNoByte, _.aByteUnset())
    testReadingUnrecognizedField(TestStructNoI16, _.anI16Unset())
    testReadingUnrecognizedField(TestStructNoI32, _.anI32Unset())
    testReadingUnrecognizedField(TestStructNoI64, _.anI64Unset())
    testReadingUnrecognizedField(TestStructNoDouble, _.aDoubleUnset())
    testReadingUnrecognizedField(TestStructNoString, _.aStringUnset())
    testReadingUnrecognizedField(TestStructNoBinary, _.aBinaryUnset())
    testReadingUnrecognizedField(TestStructNoStruct, _.aStructUnset())
    testReadingUnrecognizedField(TestStructNoSet, _.aSetUnset())
    testReadingUnrecognizedField(TestStructNoList, _.aListUnset())
    testReadingUnrecognizedField(TestStructNoMap, _.aMapUnset())

    def testReadingUnrecognizedField(oldObjMeta: MetaRecord[_], unsetOneField: TestStruct.Mutable => Unit) {
      // Write the object out.

      val buf = doWrite(writerFactory, newObj)
      // Read the new object into an older version of the same struct, to test backwards compatibility.
      val oldObj = oldObjMeta.createRawRecord.asInstanceOf[TBase[_, _]]
      doRead(readerFactory, buf, oldObj)

      // Check that we got what we expect.
      val expected = newObj.mutableCopy()
      unsetOneField(expected)
      assertEquals(expected.toString, oldObj.toString)
    }
  }

  private def doWrite(protocolFactory: TProtocolFactory, thriftObj: TBase[_, _]): TMemoryBuffer = {
    val trans = new TMemoryBuffer(1024)
    val oprot = protocolFactory.getProtocol(trans)
    thriftObj.write(oprot)
    trans
  }

  private def doRead(protocolFactory: TProtocolFactory, trans: TTransport, thriftObj: TBase[_, _]) {
    val iprot = protocolFactory.getProtocol(trans)
    thriftObj.read(iprot)
  }
}

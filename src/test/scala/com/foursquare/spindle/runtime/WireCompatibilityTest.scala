// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.test

import com.foursquare.spindle.MetaRecord
import com.foursquare.spindle.test.gen._
import java.nio.ByteBuffer
import org.apache.thrift.TBase
import org.apache.thrift.transport.{TMemoryBuffer, TTransport}
import org.junit.Assert.assertEquals
import org.junit.Test
import com.foursquare.spindle.runtime.{TProtocolInfo, KnownTProtocolNames}


class WireCompatibilityTest {

  @Test
  def testBSON2CompactCrossCompatibility() {
    // This is the most important case for us: It simulates the realistic scenario where we read records
    // from mongodb, convert them to a the compact binary protocol and put them in a serving system.
    // This verifies that unknown fields will survive that trip, so we single it out here for emphasis and
    // ease of debugging, even though this combo is also tested in testAllCompatibilityCombos().
    doTestUnknownFieldCompatibility(KnownTProtocolNames.TBSONProtocol, KnownTProtocolNames.TCompactProtocol)
  }

  @Test
  def testAllCompatibilityCombos() {
    // Test all 25 possible combinations of src and dst protocol.
    val protocols =
      KnownTProtocolNames.TBinaryProtocol ::
      KnownTProtocolNames.TCompactProtocol ::
      KnownTProtocolNames.TJSONProtocol ::
      KnownTProtocolNames.TBSONProtocol ::
      KnownTProtocolNames.TReadableJSONProtocol ::
      Nil

    for (src <- protocols; dst <- protocols) {
      println("Testing unknown field compatibility between: %s -> %s".format(src, dst))
      doTestUnknownFieldCompatibility(src, dst)
    }
  }

  private def doTestUnknownFieldCompatibility(srcProtocol: String, dstProtocol: String) {
    val obj = testStruct()

    // Test reading via versions of the struct missing one field.
    def testReadingUnknownTestStructField(oldMeta: MetaRecord[_]) = testReadingUnknownField(oldMeta, TestStruct, obj)
    testReadingUnknownTestStructField(TestStructNoBool)
    testReadingUnknownTestStructField(TestStructNoByte)
    testReadingUnknownTestStructField(TestStructNoI16)
    testReadingUnknownTestStructField(TestStructNoI32)
    testReadingUnknownTestStructField(TestStructNoI64)
    testReadingUnknownTestStructField(TestStructNoDouble)
    testReadingUnknownTestStructField(TestStructNoString)
    testReadingUnknownTestStructField(TestStructNoBinary)
    testReadingUnknownTestStructField(TestStructNoStruct)
    testReadingUnknownTestStructField(TestStructNoSet)
    testReadingUnknownTestStructField(TestStructNoList)
    testReadingUnknownTestStructField(TestStructNoMap)

    // Test reading various structs via a struct with no fields at all, so all fields are unknown.
    testReadingUnknownField(StructWithNoFields, TestStruct, testStruct())
    testReadingUnknownField(StructWithNoFields, TestStructCollections, testStructCollections())
    testReadingUnknownField(StructWithNoFields, TestStructNestedCollections, testStructNestedCollections())

    // Test that unknown values in known enum fields are preserved.
    testReadingUnknownField(StructWithOldEnumField, StructWithNewEnumField, testEnumStruct())

    // "new" vs. "old" here mean "struct with all fields" vs. "struct without some fields".
    def testReadingUnknownField(oldMeta: MetaRecord[_], newMeta: MetaRecord[_], newObj: TBase[_, _]) {
      // Write the object out.
      val newBuf = doWrite(srcProtocol, newObj)

      // Read the new object into an older version of the same struct.
      val oldObj = oldMeta.createRawRecord.asInstanceOf[TBase[_, _]]
      doRead(srcProtocol, newBuf, oldObj)

      // Write the old object back out.
      val oldBuf = doWrite(dstProtocol, oldObj)

      // Read it back into a fresh instance of the new version of the struct.
      val roundtrippedNewObj = newMeta.createRawRecord.asInstanceOf[TBase[_, _]]
      doRead(dstProtocol, oldBuf, roundtrippedNewObj)

      // Check that we got what we expect.
      assertEquals(newObj, roundtrippedNewObj)
    }
  }

  private def doWrite(protocolName: String, thriftObj: TBase[_, _]): TMemoryBuffer = {
    val protocolFactory = TProtocolInfo.getWriterFactory(protocolName)
    val trans = new TMemoryBuffer(1024)
    val oprot = protocolFactory.getProtocol(trans)
    thriftObj.write(oprot)
    trans
  }

  private def doRead(protocolName: String, trans: TTransport, thriftObj: TBase[_, _]) {
    val protocolFactory = TProtocolInfo.getReaderFactory(protocolName)
    val iprot = protocolFactory.getProtocol(trans)
    thriftObj.read(iprot)
  }

  private def testStruct() = TestStruct.newBuilder
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

  private def testStructCollections() = TestStructCollections.newBuilder
    .listBool(List[Boolean](true, false, true))
    .listByte(List[Byte](0, 1, -1, 42, -9, Byte.MinValue, Byte.MaxValue))
    .listI16(List[Short](0, 1, -1, 42, -9, Short.MinValue, Short.MaxValue))
    .listI32(List[Int](0, 1, -1, 42, -9, Int.MinValue, Int.MaxValue))
    .listI64(List[Long](0, 1, -1, 42, -9, Long.MinValue, Long.MaxValue))
    .listDouble(List[Double](0f, 1f, -1f, -32.7, Double.MinValue, Double.MaxValue, Double.MinPositiveValue))
    .listString(List[String]("hello", "world"))
    .listBinary(List[ByteBuffer](ByteBuffer.wrap(Array[Byte](1, 2, 3)), ByteBuffer.wrap(Array[Byte](65, 12))))
    .listStruct(List[InnerStruct](InnerStruct("hi", 5), InnerStruct("bye", 6)))
    .setBool(Set[Boolean](true, false, true))
    .setByte(Set[Byte](0, 1, -1, 42, -9, Byte.MinValue, Byte.MaxValue))
    .setI16(Set[Short](0, 1, -1, 42, -9, Short.MinValue, Short.MaxValue))
    .setI32(Set[Int](0, 1, -1, 42, -9, Int.MinValue, Int.MaxValue))
    .setI64(Set[Long](0, 1, -1, 42, -9, Long.MinValue, Long.MaxValue))
    .setDouble(Set[Double](0f, 1f, -1f, -32.7, Double.MinValue, Double.MaxValue, Double.MinPositiveValue))
    .setString(Set[String]("hello", "world"))
    .setBinary(Set[ByteBuffer](ByteBuffer.wrap(Array[Byte](1, 2, 3)), ByteBuffer.wrap(Array[Byte](65, 12))))
    .setStruct(Set[InnerStruct](InnerStruct("hi", 5), InnerStruct("bye", 6)))
    .mapBool(Map[String, Boolean]("a" -> true, "b" -> false, "c" -> true))
    .mapByte(Map[String, Byte]("a" -> 0, "b" -> 1, "c" -> -1, "d" -> 42, "e" -> -9, "f" -> Byte.MinValue, "g" -> Byte.MaxValue))
    .mapI16(Map[String, Short]("a" -> 0, "b" -> 1, "c" -> -1, "d" -> 42, "e" -> -9, "f" -> Short.MinValue, "g" -> Short.MaxValue))
    .mapI32(Map[String, Int]("a" -> 0, "b" -> 1, "c" -> -1, "d" -> 42, "e" -> -9, "f" -> Int.MinValue, "g" -> Int.MaxValue))
    .mapI64(Map[String, Long]("a" -> 0, "b" -> 1, "c" -> -1, "d" -> 42, "e" -> -9, "f" -> Long.MinValue, "g" -> Long.MaxValue))
    .mapDouble(Map[String, Double]("a" -> 0f, "b" -> 1f, "c" -> -1f, "d" -> -32.7, "e" -> Double.MinValue, "f" -> Double.MaxValue, "g" -> Double.MinPositiveValue))
    .mapString(Map[String, String]("a" -> "hello", "b" -> "world"))
    .mapBinary(Map[String, ByteBuffer]("a" -> ByteBuffer.wrap(Array[Byte](1, 2, 3)), "b" -> ByteBuffer.wrap(Array[Byte](65, 12))))
    .mapStruct(Map[String, InnerStruct]("a" -> InnerStruct("hi", 5), "b" -> InnerStruct("bye", 6)))
    .result()

  private def testStructNestedCollections() = TestStructNestedCollections.newBuilder
    .listBool(List[List[Boolean]](List[Boolean](true, false), List[Boolean](true)))
    .listByte(List[List[Byte]](List[Byte](0, 1, -1, 42), List[Byte](-9, Byte.MinValue, Byte.MaxValue)))
    .listI16(List[List[Short]](List[Short](), List[Short](0, 1, -1, 42, -9, Short.MinValue, Short.MaxValue)))
    .listI32(List[List[Int]](List[Int](0, 1, -1, 42, -9, Int.MinValue, Int.MaxValue), List[Int]()))
    .listI64(List[List[Long]](List[Long](0), List[Long](1, -1, 42, -9, Long.MinValue, Long.MaxValue)))
    .listDouble(List[List[Double]](List[Double](0f, 1f, -1f, -32.7, Double.MinValue), List[Double](Double.MaxValue, Double.MinPositiveValue)))
    .listString(List[List[String]](List[String]("hello", "world")))
    .listBinary(List[List[ByteBuffer]](List[ByteBuffer](ByteBuffer.wrap(Array[Byte](1, 2, 3))), List[ByteBuffer](ByteBuffer.wrap(Array[Byte](65, 12)))))
    .listStruct(List[List[InnerStruct]](List[InnerStruct](), List[InnerStruct](InnerStruct("hi", 5), InnerStruct("bye", 6))))
    .setBool(Set[Set[Boolean]](Set[Boolean](true, false), Set[Boolean](true)))
    .setByte(Set[Set[Byte]](Set[Byte](0, 1, -1, 42), Set[Byte](-9, Byte.MinValue, Byte.MaxValue)))
    .setI16(Set[Set[Short]](Set[Short](), Set[Short](0, 1, -1, 42, -9, Short.MinValue, Short.MaxValue)))
    .setI32(Set[Set[Int]](Set[Int](0, 1, -1, 42, -9, Int.MinValue, Int.MaxValue), Set[Int]()))
    .setI64(Set[Set[Long]](Set[Long](0), Set[Long](1, -1, 42, -9, Long.MinValue, Long.MaxValue)))
    .setDouble(Set[Set[Double]](Set[Double](0f, 1f, -1f, -32.7, Double.MinValue), Set[Double](Double.MaxValue, Double.MinPositiveValue)))
    .setString(Set[Set[String]](Set[String]("hello", "world")))
    .setBinary(Set[Set[ByteBuffer]](Set[ByteBuffer](ByteBuffer.wrap(Array[Byte](1, 2, 3))), Set[ByteBuffer](ByteBuffer.wrap(Array[Byte](65, 12)))))
    .setStruct(Set[Set[InnerStruct]](Set[InnerStruct](), Set[InnerStruct](InnerStruct("hi", 5), InnerStruct("bye", 6))))
    .mapBool(Map[String, Map[String, Boolean]]("foo" -> Map[String, Boolean]("a" -> true, "b" -> false), "bar" -> Map[String, Boolean]("c" -> true)))
    .mapByte(Map[String, Map[String, Byte]]("foo" -> Map[String, Byte]("a" -> 0, "b" -> 1, "c" -> -1, "d" -> 42), "bar" -> Map[String, Byte]("e" -> -9, "f" -> Byte.MinValue, "g" -> Byte.MaxValue)))
    .mapI16(Map[String, Map[String, Short]]("foo" -> Map[String, Short](), "bar" -> Map[String, Short]("a" -> 0, "b" -> 1, "c" -> -1, "d" -> 42, "e" -> -9, "f" -> Short.MinValue, "g" -> Short.MaxValue)))
    .mapI32(Map[String, Map[String, Int]]("foo" -> Map[String, Int]("a" -> 0, "b" -> 1, "c" -> -1, "d" -> 42, "e" -> -9, "f" -> Int.MinValue, "g" -> Int.MaxValue), "bar" -> Map[String, Int]()))
    .mapI64(Map[String, Map[String, Long]]("foo" -> Map[String, Long]("a" -> 0), "bar" -> Map[String, Long]("b" -> 1, "c" -> -1, "d" -> 42, "e" -> -9, "f" -> Long.MinValue, "g" -> Long.MaxValue)))
    .mapDouble(Map[String, Map[String, Double]]("foo" -> Map[String, Double]("a" -> 0f, "b" -> 1f, "c" -> -1f, "d" -> -32.7, "e" -> Double.MinValue), "bar" -> Map[String, Double]("f" -> Double.MaxValue, "g" -> Double.MinPositiveValue)))
    .mapString(Map[String, Map[String, String]]("foo" -> Map[String, String]("a"-> "hello", "b" -> "world")))
    .mapBinary(Map[String, Map[String, ByteBuffer]]("foo" -> Map[String, ByteBuffer]("a" -> ByteBuffer.wrap(Array[Byte](1, 2, 3))), "bar" -> Map[String, ByteBuffer]("b" -> ByteBuffer.wrap(Array[Byte](65, 12)))))
    .mapStruct(Map[String, Map[String, InnerStruct]]("foo" -> Map[String, InnerStruct](), "bar" -> Map[String, InnerStruct]("a" -> InnerStruct("hi", 5), "b" -> InnerStruct("bye", 6))))
    .result()

  private def testEnumStruct() = StructWithNewEnumField.newBuilder
    .anEnum(NewTestEnum.Two)
    .anEnumList(NewTestEnum.Zero :: NewTestEnum.Two :: NewTestEnum.One :: Nil)
    .result()
}

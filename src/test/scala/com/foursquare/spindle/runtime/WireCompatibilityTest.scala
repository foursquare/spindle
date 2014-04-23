// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.test

import com.foursquare.spindle.MetaRecord
import com.foursquare.spindle.test.gen._
import java.nio.ByteBuffer
import org.apache.thrift.TBase
import org.apache.thrift.transport.{AutoExpandingBufferReadTransport, TMemoryBuffer}
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
    val protocols = List(
      KnownTProtocolNames.TBinaryProtocol,
      KnownTProtocolNames.TCompactProtocol,
      KnownTProtocolNames.TJSONProtocol,
      KnownTProtocolNames.TBSONProtocol,
      KnownTProtocolNames.TReadableJSONProtocol)

    for (src <- protocols; dst <- protocols) {
      println("Testing unknown field compatibility between: %s -> %s".format(src, dst))
      doTestUnknownFieldCompatibility(src, dst)
    }
  }

  private def doTestUnknownFieldCompatibility(srcProtocol: String, dstProtocol: String) {
    val s = testStruct()
    val sNoBool = s.toBuilder.aBool(None).result()
    val sNoUnknownFields = testStructNoUnknownFieldsTracking()
    val sNoUnknownFieldsExceptInner = testStructNoUnknownFieldsTrackingExceptInner()
    val sInnerStructNoUnknownFields = testStructInnerStructNoUnknownFieldsTracking()
    val sInnerStructNoStringNoUnknownFields = testStructInnerStructNoStringNoUnknownFieldsTracking()
    val sNoUnknownFieldsExceptInnerNoString = testStructNoUnknownFieldsTrackingExceptInnerStructNoString()
    val sNoUnknownFieldsNoBool = sNoUnknownFields.toBuilder.aBool(None).result()
    val sInnerStructNoString = testStructInnerStructNoString()
    val sInnerStructNoI32 = testStructInnerStructNoI32()
    val sNoUnknownFieldsInnerStructNoString = testStructNoUnknownFieldsTrackingInnerStructNoString()
    val sCollections = testStructCollections()
    val sNestedCollections = testStructNestedCollections()

    // Test reading via versions of the struct missing one field.
    val structsMissingOneField: List[MetaRecord[_]] = List(
      TestStructNoBool,
      TestStructNoByte,
      TestStructNoI16,
      TestStructNoI32,
      TestStructNoI64,
      TestStructNoDouble,
      TestStructNoString,
      TestStructNoBinary,
      TestStructNoStruct,
      TestStructNoSet,
      TestStructNoList,
      TestStructNoMap,
      TestStructNoMyBinary,
      TestStructInnerStructNoString,
      TestStructInnerStructNoI32)

    for (oldMeta <- structsMissingOneField) {
      testReadingUnknownField(oldMeta, TestStruct, s)
    }

    // Test reading various structs via a struct with no fields at all, so all fields are unknown.
    testReadingUnknownField(StructWithNoFields, TestStruct, s)
    testReadingUnknownField(StructWithNoFields, TestStructCollections, sCollections)
    testReadingUnknownField(StructWithNoFields, TestStructNestedCollections, sNestedCollections)

    // Test using versions of the struct with and without unknown fields tracking enabled.
    testReadingUnknownField(TestStructNoUnknownFieldsTracking, TestStruct, s)
    testReadingUnknownField(TestStruct, TestStructNoUnknownFieldsTracking, sNoUnknownFields)
    testReadingUnknownField(TestStructNoBoolNoUnknownFieldsTracking, TestStruct, s,
      Some(sNoBool), Some(sNoBool))
    testReadingUnknownField(TestStructNoBool, TestStructNoUnknownFieldsTracking, sNoUnknownFields,
      Some(sNoUnknownFieldsNoBool), Some(sNoUnknownFields))
    testReadingUnknownField(TestStructNoBoolNoUnknownFieldsTracking, TestStructNoUnknownFieldsTracking, sNoUnknownFields,
      Some(sNoUnknownFieldsNoBool), Some(sNoUnknownFieldsNoBool))

    // Test using versions of the struct with retired fields, with and without unknown fields tracking enabled.
    testReadingUnknownField(TestStructNoBoolRetiredFields, TestStruct, s,
      Some(sNoBool), Some(sNoBool))
    testReadingUnknownField(TestStructNoBoolRetiredFieldsNoUnknownFieldsTracking, TestStruct, s,
      Some(sNoBool), Some(sNoBool))
    testReadingUnknownField(TestStructNoBoolRetiredFields, TestStructNoUnknownFieldsTracking, sNoUnknownFields,
      Some(sNoUnknownFieldsNoBool), Some(sNoUnknownFieldsNoBool))
    testReadingUnknownField(TestStructNoBoolRetiredFieldsNoUnknownFieldsTracking, TestStructNoUnknownFieldsTracking, sNoUnknownFields,
      Some(sNoUnknownFieldsNoBool), Some(sNoUnknownFieldsNoBool))
    testReadingUnknownField(TestStructInnerStructNoI32RetiredFields, TestStruct, s,
      Some(sInnerStructNoI32), Some(sInnerStructNoI32))

    // Test using versions of the struct with and without unknown fields enabled on embedded structs.
    // 16 cases here. There are 4 independent places to enable or disable unknown fields tracking:
    //   - new version of the struct
    //   - inner struct of the new version of the struct
    //   - old version of the struct
    //   - inner struct of the old version of the struct
    testReadingUnknownField(TestStructInnerStructNoString, TestStruct, s)
    testReadingUnknownField(TestStructNoUnknownFieldsTrackingInnerStructNoString, TestStruct, s)
    testReadingUnknownField(TestStructInnerStructNoStringNoUnknownFieldsTracking, TestStruct, s,
      Some(sInnerStructNoString), Some(sInnerStructNoString))
    testReadingUnknownField(TestStructNoUnknownFieldsTrackingInnerStructNoStringNoUnknownFieldsTracking, TestStruct, s,
      Some(sInnerStructNoString), Some(sInnerStructNoString))

    testReadingUnknownField(TestStructInnerStructNoString, TestStructNoUnknownFieldsTrackingExceptInnerStruct, sNoUnknownFieldsExceptInner)
    testReadingUnknownField(TestStructNoUnknownFieldsTrackingInnerStructNoString, TestStructNoUnknownFieldsTrackingExceptInnerStruct, sNoUnknownFieldsExceptInner)
    testReadingUnknownField(TestStructInnerStructNoStringNoUnknownFieldsTracking, TestStructNoUnknownFieldsTrackingExceptInnerStruct, sNoUnknownFieldsExceptInner,
      Some(sNoUnknownFieldsExceptInnerNoString), Some(sNoUnknownFieldsExceptInnerNoString))
    testReadingUnknownField(TestStructNoUnknownFieldsTrackingInnerStructNoStringNoUnknownFieldsTracking, TestStructNoUnknownFieldsTrackingExceptInnerStruct, sNoUnknownFieldsExceptInner,
      Some(sNoUnknownFieldsExceptInnerNoString), Some(sNoUnknownFieldsExceptInnerNoString))

    testReadingUnknownField(TestStructInnerStructNoString, TestStructInnerStructNoUnknownFieldsTracking, sInnerStructNoUnknownFields,
      Some(sInnerStructNoStringNoUnknownFields), Some(sInnerStructNoUnknownFields))
    testReadingUnknownField(TestStructNoUnknownFieldsTrackingInnerStructNoString, TestStructInnerStructNoUnknownFieldsTracking, sInnerStructNoUnknownFields,
      Some(sInnerStructNoStringNoUnknownFields), Some(sInnerStructNoUnknownFields))
    testReadingUnknownField(TestStructInnerStructNoStringNoUnknownFieldsTracking, TestStructInnerStructNoUnknownFieldsTracking, sInnerStructNoUnknownFields,
      Some(sInnerStructNoStringNoUnknownFields), Some(sInnerStructNoStringNoUnknownFields))
    testReadingUnknownField(TestStructNoUnknownFieldsTrackingInnerStructNoStringNoUnknownFieldsTracking, TestStructInnerStructNoUnknownFieldsTracking, sInnerStructNoUnknownFields,
      Some(sInnerStructNoStringNoUnknownFields), Some(sInnerStructNoStringNoUnknownFields))

    testReadingUnknownField(TestStructInnerStructNoString, TestStructNoUnknownFieldsTracking, sNoUnknownFields,
      Some(sNoUnknownFieldsInnerStructNoString), Some(sNoUnknownFields))
    testReadingUnknownField(TestStructNoUnknownFieldsTrackingInnerStructNoString, TestStructNoUnknownFieldsTracking, sNoUnknownFields,
      Some(sNoUnknownFieldsInnerStructNoString), Some(sNoUnknownFields))
    testReadingUnknownField(TestStructInnerStructNoStringNoUnknownFieldsTracking, TestStructNoUnknownFieldsTracking, sNoUnknownFields,
      Some(sNoUnknownFieldsInnerStructNoString), Some(sNoUnknownFieldsInnerStructNoString))
    testReadingUnknownField(TestStructNoUnknownFieldsTrackingInnerStructNoStringNoUnknownFieldsTracking, TestStructNoUnknownFieldsTracking, sNoUnknownFields,
      Some(sNoUnknownFieldsInnerStructNoString), Some(sNoUnknownFieldsInnerStructNoString))

    // TODO(benjy): Test that unknown values in known enum fields are preserved.
    // testReadingUnknownField(StructWithOldEnumField, StructWithNewEnumField, testEnumStruct())

    // "new" vs. "old" here mean "struct with all fields" vs. "struct without some fields".
    def testReadingUnknownField(oldMeta: MetaRecord[_], newMeta: MetaRecord[_], newObj: TBase[_, _],
        expectedRoundTrippedObj: Option[TBase[_, _]] = None, expectedRoundTrippedObjSameProto: Option[TBase[_, _]] = None) {
      // Write the object out.
      val newBuf = doWrite(srcProtocol, newObj)

      // Read the new object into an older version of the same struct.
      val oldObj = oldMeta.createRawRecord.asInstanceOf[TBase[_, _]]
      doRead(srcProtocol, newBuf, oldObj)

      // Write the old object back out.
      val oldBuf1 = doWrite(dstProtocol, oldObj)

      // Read it back into a fresh instance of the old version of the struct.
      val roundtrippedOldObj = oldMeta.createRawRecord.asInstanceOf[TBase[_, _]]
      doRead(dstProtocol, oldBuf1, roundtrippedOldObj)

      // Check that we got what we expect.
      assertEquals(oldObj, roundtrippedOldObj)

      // Write the old object back out.
      val oldBuf2 = doWrite(dstProtocol, oldObj)

      // Read it back into a fresh instance of the new version of the struct.
      val roundtrippedNewObj = newMeta.createRawRecord.asInstanceOf[TBase[_, _]]
      doRead(dstProtocol, oldBuf2, roundtrippedNewObj)

      // Check that we got what we expect.
      val expected = {
        if (srcProtocol == dstProtocol ||
          TProtocolInfo.isRobust(srcProtocol) && TProtocolInfo.isRobust(dstProtocol)) {
          expectedRoundTrippedObjSameProto.getOrElse(newObj)
        } else {
          expectedRoundTrippedObj.getOrElse(newObj)
        }
      }
      assertEquals(expected, roundtrippedNewObj)
    }
  }

  private def doWrite(protocolName: String, thriftObj: TBase[_, _]): TMemoryBuffer = {
    val protocolFactory = TProtocolInfo.getWriterFactory(protocolName)
    val trans = new TMemoryBuffer(1024)
    val oprot = protocolFactory.getProtocol(trans)
    thriftObj.write(oprot)
    trans
  }

  private def doRead(protocolName: String, buf: TMemoryBuffer, thriftObj: TBase[_, _]) {
    val protocolFactory = TProtocolInfo.getReaderFactory(protocolName)
    // Unlike TMemoryBuffer, AutoExpandingBufferReadTransport exposes its underlying buffer, and
    // protocols such as TBinaryProtocol take advantage of this, e.g., to return binary values
    // as ByteBuffer wrappers around a segment of that underlying buffer instead of creating a copy.
    // We wrap our data in AutoExpandingBufferReadTransport here to test that things work in such cases.
    val trans = new AutoExpandingBufferReadTransport(1024, 2)
    trans.fill(buf, buf.length)
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
    .aStruct(InnerStruct("hi!", 5))
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStruct("one", 1), "dos" -> InnerStruct("two", 2)))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStruct("hello", 4), InnerStruct("world", 2)))
    .result()

  private def testStructNoUnknownFieldsTracking() = TestStructNoUnknownFieldsTracking.newBuilder
    .aBool(true)
    .aByte(120.toByte)
    .anI16(30000.toShort)
    .anI32(7654321)
    .anI64(987654321L)
    .aDouble(0.57)
    .aString("hello, world")
    .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
    .aStruct(InnerStructNoUnknownFieldsTracking("hi!", 5))
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStructNoUnknownFieldsTracking("one", 1), "dos" -> InnerStructNoUnknownFieldsTracking("two", 2)))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStructNoUnknownFieldsTracking("hello", 4), InnerStructNoUnknownFieldsTracking("world", 2)))
    .result()

  private def testStructNoUnknownFieldsTrackingExceptInner() = TestStructNoUnknownFieldsTrackingExceptInnerStruct.newBuilder
    .aBool(true)
    .aByte(120.toByte)
    .anI16(30000.toShort)
    .anI32(7654321)
    .anI64(987654321L)
    .aDouble(0.57)
    .aString("hello, world")
    .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
    .aStruct(InnerStruct("hi!", 5))
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStruct("one", 1), "dos" -> InnerStruct("two", 2)))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStruct("hello", 4), InnerStruct("world", 2)))
    .result()

  private def testStructInnerStructNoUnknownFieldsTracking() = TestStructInnerStructNoUnknownFieldsTracking.newBuilder
    .aBool(true)
    .aByte(120.toByte)
    .anI16(30000.toShort)
    .anI32(7654321)
    .anI64(987654321L)
    .aDouble(0.57)
    .aString("hello, world")
    .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
    .aStruct(InnerStructNoUnknownFieldsTracking("hi!", 5))
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStructNoUnknownFieldsTracking("one", 1), "dos" -> InnerStructNoUnknownFieldsTracking("two", 2)))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStructNoUnknownFieldsTracking("hello", 4), InnerStructNoUnknownFieldsTracking("world", 2)))
    .result()

  private def testStructInnerStructNoString() = TestStruct.newBuilder
    .aBool(true)
    .aByte(120.toByte)
    .anI16(30000.toShort)
    .anI32(7654321)
    .anI64(987654321L)
    .aDouble(0.57)
    .aString("hello, world")
    .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
    .aStruct(InnerStruct.newBuilder.anInt(5).result())
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStruct.newBuilder.anInt(1).result(), "dos" -> InnerStruct.newBuilder.anInt(2).result()))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStruct.newBuilder.anInt(4).result(), InnerStruct.newBuilder.anInt(2).result()))
    .result()

  private def testStructNoUnknownFieldsTrackingInnerStructNoString() = TestStructNoUnknownFieldsTracking.newBuilder
    .aBool(true)
    .aByte(120.toByte)
    .anI16(30000.toShort)
    .anI32(7654321)
    .anI64(987654321L)
    .aDouble(0.57)
    .aString("hello, world")
    .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
    .aStruct(InnerStructNoUnknownFieldsTracking.newBuilder.anInt(5).result())
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStructNoUnknownFieldsTracking.newBuilder.anInt(1).result(), "dos" -> InnerStructNoUnknownFieldsTracking.newBuilder.anInt(2).result()))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStructNoUnknownFieldsTracking.newBuilder.anInt(4).result(), InnerStructNoUnknownFieldsTracking.newBuilder.anInt(2).result()))
    .result()

  private def testStructInnerStructNoStringNoUnknownFieldsTracking() = TestStructInnerStructNoUnknownFieldsTracking.newBuilder
    .aBool(true)
    .aByte(120.toByte)
    .anI16(30000.toShort)
    .anI32(7654321)
    .anI64(987654321L)
    .aDouble(0.57)
    .aString("hello, world")
    .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
    .aStruct(InnerStructNoUnknownFieldsTracking.newBuilder.anInt(5).result())
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStructNoUnknownFieldsTracking.newBuilder.anInt(1).result(), "dos" -> InnerStructNoUnknownFieldsTracking.newBuilder.anInt(2).result()))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStructNoUnknownFieldsTracking.newBuilder.anInt(4).result(), InnerStructNoUnknownFieldsTracking.newBuilder.anInt(2).result()))
    .result()

  private def testStructNoUnknownFieldsTrackingExceptInnerStructNoString() = TestStructNoUnknownFieldsTrackingExceptInnerStruct.newBuilder
    .aBool(true)
    .aByte(120.toByte)
    .anI16(30000.toShort)
    .anI32(7654321)
    .anI64(987654321L)
    .aDouble(0.57)
    .aString("hello, world")
    .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
    .aStruct(InnerStruct.newBuilder.anInt(5).result())
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStruct.newBuilder.anInt(1).result(), "dos" -> InnerStruct.newBuilder.anInt(2).result()))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStruct.newBuilder.anInt(4).result(), InnerStruct.newBuilder.anInt(2).result()))
    .result()

  private def testStructInnerStructNoI32() = TestStruct.newBuilder
    .aBool(true)
    .aByte(120.toByte)
    .anI16(30000.toShort)
    .anI32(7654321)
    .anI64(987654321L)
    .aDouble(0.57)
    .aString("hello, world")
    .aBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5)))
    .aStruct(InnerStruct.newBuilder.aString("hi!").result())
    .aSet(Set("foo", "bar", "baz"))
    .aList(List(4, 8, 15, 16, 23, 42))
    .aMap(Map("uno" -> InnerStruct.newBuilder.aString("one").result(), "dos" -> InnerStruct.newBuilder.aString("two").result()))
    .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
    .aStructList(Seq(InnerStruct.newBuilder.aString("hello").result(), InnerStruct.newBuilder.aString("world").result()))
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

  private def testEnumStruct() = StructWithNewEnumField.newBuilder.anEnum(NewTestEnum.Two).result()
}

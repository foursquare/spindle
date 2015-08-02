// Copyright 2015 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.common.thrift.bson

import org.junit._

import com.mongodb.{BasicDBObjectBuilder, DBObject}
import com.foursquare.spindle.{UntypedFieldDescriptor, UntypedMetaRecord, UntypedRecord}
import com.foursquare.spindle.test.gen.{InnerStruct, RawTestStruct, TestStruct, TestStructNestedCollections}
import org.bson.BasicBSONEncoder
import org.bson.types.ObjectId
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

class TBSONBinaryProtocolTest {

  def assertRoundTrip(record: UntypedRecord) {
    val protocolFactory = new TBSONObjectProtocol.WriterFactoryForDBObject
    val writeProtocol = protocolFactory.getProtocol
    record.write(writeProtocol)
    val dbo = writeProtocol.getOutput.asInstanceOf[DBObject]

    val encoder = new BasicBSONEncoder()
    val bytes: Array[Byte] = encoder.encode(dbo)
    val newRecord = record.meta.createUntypedRawRecord
    val protocol = new TBSONBinaryProtocol()
    protocol.setSource(new ByteArrayInputStream(bytes))
    newRecord.read(protocol)
    Assert.assertEquals(record, newRecord)

  }

  @Test
  def testBasicFields {
    
    val struct = TestStruct.newBuilder
      .aByte(12.toByte)
      .anI16(1234.toShort)
      .anI32(123456)
      .anI64(123456789999L)
      .aDouble(123456.123456)
      .aString("hello, how are you today?")
      .aBinary(ByteBuffer.wrap("foobar".getBytes("UTF-8")))
      .aStruct(InnerStruct("inner hello", 1234567))
      .aSet(Set("1","2","3","4","5"))
      .aList(List(1,2,3,4,5))
      .aMap(
        Map("one" -> InnerStruct("inner in map one", 1),
            "two" -> InnerStruct("inner in map two", 2)
        )
      )
      .aMyBinary(ByteBuffer.wrap(Array[Byte](1, 2, 3, 4, 5, 6)))
      .aStructList(List(
        InnerStruct("inner in list one", 1),
        InnerStruct("inner in list one", 2)
      )).result()

    assertRoundTrip(struct)
  }

  @Test
  def testNestedStruct {
    
    val struct = TestStructNestedCollections.newBuilder
      .listBool(List(List(true, false, true)))
      .listStruct(List(List(
        InnerStruct("inner in list one", 1),
        InnerStruct("inner in list one", 2)
      )))
      .mapBool(Map("outer" -> Map("inner" -> true)))
      .mapStruct(Map("outer" -> Map("inner" -> InnerStruct("inner in list one", 1))))
      .result()

    assertRoundTrip(struct)
  }

  @Test
  def testMongoError {
    val message = "Things have gone very poorly"
    val dbo: DBObject = BasicDBObjectBuilder.start().add("$err", message).add("code", 123).get
    val encoder = new BasicBSONEncoder()
    val bytes: Array[Byte] = encoder.encode(dbo)
    val newRecord = new RawTestStruct()
    val protocol = new TBSONBinaryProtocol()
    protocol.setSource(new ByteArrayInputStream(bytes))
    newRecord.read(protocol)
    Assert.assertEquals(message, protocol.errorMessage)
    Assert.assertEquals(123, protocol.errorCode)
  }

}
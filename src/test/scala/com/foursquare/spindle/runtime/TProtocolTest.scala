// Copyright 2014 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.runtime.test

import com.foursquare.common.thrift.json.TReadableJSONProtocol
import com.foursquare.common.thrift.bson.TBSONObjectProtocol
import com.foursquare.spindle.{Record, MetaRecord}
import com.foursquare.spindle.test.gen.{TestStruct, TestStructNoBoolRetiredFields, TestStructNoUnknownFieldsTracking}
import com.mongodb.{BasicDBObjectBuilder, DBObject}
import org.apache.thrift.TBase
import org.apache.thrift.transport.TMemoryInputTransport
import org.junit.Assert.assertEquals
import org.junit.{Test, Ignore}

class TProtocolTest {

  @Test
  def testJsonNullValues {
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

  def deserializeJson[R <: Record[R] with TBase[R, _ <: org.apache.thrift.TFieldIdEnum]](s: String, recMeta: MetaRecord[R]): R = {
    val deserializer = new TDeserializer(new TReadableJSONProtocol.Factory())
    val rec = recMeta.createRawRecord
    deserializer.deserialize(rec, s.getBytes("UTF-8"))
    rec
  }

  @Test @Ignore
  def testBsonHLists {
    val hlist = new java.util.ArrayList[Object]
    hlist.add("hi")
    hlist.add(new java.lang.Integer(1))

    val dboHList = BasicDBObjectBuilder.start.add("anI32", 3).add("aList", hlist).get
    val dboUnknownHList = BasicDBObjectBuilder.start.add("anI32", 3).add("tombstones", hlist).get
    val dboRetiredHList = BasicDBObjectBuilder.start.add("anI32", 3).add("aBool", hlist).get

    val s = TestStruct.newBuilder.anI32(3).result()
    val sNoTracking = TestStructNoUnknownFieldsTracking.newBuilder.anI32(3).result()
    val sRetired = TestStructNoBoolRetiredFields.newBuilder.anI32(3).result()

    val s1 = readBson(dboHList, TestStruct)
    assertEquals(s1, s)

    val s2 = readBson(dboUnknownHList, TestStruct)
    assertEquals(s2, s)

    val s3 = readBson(dboHList, TestStructNoUnknownFieldsTracking)
    assertEquals(s3, sNoTracking)

    val s4 = readBson(dboUnknownHList, TestStructNoUnknownFieldsTracking)
    assertEquals(s4, sNoTracking)

    val s5 = readBson(dboRetiredHList, TestStructNoBoolRetiredFields)
    assertEquals(s5, sRetired)
  }

  def readBson[R <: Record[R] with TBase[R, _]](dbo: DBObject, recMeta: MetaRecord[R]): R = {
    val record = recMeta.createRawRecord
    val protocolFactory = new TBSONObjectProtocol.ReaderFactory
    val protocol = protocolFactory.getProtocol
    protocol.setSource(dbo)
    record.read(protocol)
    record
  }
}
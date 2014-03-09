// Copyright 2014 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.runtime.test

import com.foursquare.spindle.test.gen.MapWithObjectIdKeys
import org.bson.types.ObjectId
import org.apache.thrift.TBase
import org.apache.thrift.transport.{TTransport, TMemoryBuffer}
import com.foursquare.spindle.runtime.{KnownTProtocolNames, TProtocolInfo}


import org.junit.Assert.assertEquals
import org.junit.Test


class EnhancedTypesTest {

  @Test
  def testObjectIdMapKeys() {
    val protocols =
      KnownTProtocolNames.TBinaryProtocol ::
      KnownTProtocolNames.TCompactProtocol ::
      KnownTProtocolNames.TJSONProtocol ::
      KnownTProtocolNames.TBSONProtocol ::
      KnownTProtocolNames.TReadableJSONProtocol ::
      Nil

    for (tproto <- protocols) {
      println("Testing enhanced types for protocol %s".format(tproto))
      doTestObjectIdMapKeys(tproto)
    }
  }

  private def doTestObjectIdMapKeys(tproto: String) {
    val m = Map(new ObjectId() -> 4.5, new ObjectId() -> 33.7)
    val struct = MapWithObjectIdKeys.newBuilder.foo(m).result()

    // Write the object out.
    val buf = doWrite(tproto, struct)

    // Read the new object into an older version of the same struct.
    val roundtrippedStruct = MapWithObjectIdKeys.createRawRecord.asInstanceOf[TBase[_, _]]
    doRead(tproto, buf, roundtrippedStruct)

    assertEquals(struct, roundtrippedStruct)
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
}

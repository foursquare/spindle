package com.foursquare.spindle.runtime

import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol, TJSONProtocol, TProtocol, TProtocolFactory}
import com.foursquare.common.thrift.bson.{TBSONProtocol, TBSONBinaryProtocol}
import com.foursquare.common.thrift.json.TReadableJSONProtocol


object KnownTProtocolNames {
  val TBinaryProtocol = "org.apache.thrift.protocol.TBinaryProtocol"
  val TCompactProtocol = "org.apache.thrift.protocol.TCompactProtocol"
  val TJSONProtocol = "org.apache.thrift.protocol.TJSONProtocol"
  val TBSONProtocol = "com.foursquare.common.thrift.bson.TBSONProtocol"
  val TReadableJSONProtocol = "com.foursquare.common.thrift.json.TReadableJSONProtocol"
  val TBSONBinaryProtocol = "com.foursquare.common.thrift.bson.TBSONBinaryProtocol"
}

// Utilities related to known TProtocol implementations.
object TProtocolInfo {
  // Returns the name of the given protocol.
  def getProtocolName(prot: TProtocol): String = prot.getClass.getCanonicalName match {
    // When reading/writing from a BSON object we handle unknown fields as TBSONProtocol.
    case "com.foursquare.common.thrift.bson.TBSONObjectProtocol" => KnownTProtocolNames.TBSONProtocol
    case s => s
  }

  // Returns true if the protocol uses precise type information and field ids on the wire.
  //
  // Non-robust protocols may use field names on the wire instead of ids, and/or
  // may use imprecise type information, such as representing an i16 as a 32-bit int.
  //
  // Note that TJSONProtocol does have full type and field id information, but it represents
  // binary fields as Base64-encoded strings, and this causes it to interoperate incorrectly
  // with the binary protocols when writing inline.  So we consider it non-robust.
  def isRobust(protocolName: String): Boolean = protocolName match {
    case KnownTProtocolNames.TBinaryProtocol |
         KnownTProtocolNames.TCompactProtocol => true
    case _ => false
  }

  def isTextBased(protocolName: String): Boolean = protocolName match {
    case KnownTProtocolNames.TJSONProtocol |
         KnownTProtocolNames.TReadableJSONProtocol => true
    case _ => false
  }

  def getReaderFactory(protocolName: String): TProtocolFactory = protocolName match {
    case KnownTProtocolNames.TBSONProtocol => new TBSONProtocol.ReaderFactory()
    case KnownTProtocolNames.TBSONBinaryProtocol => new TBSONBinaryProtocol.ReaderFactory()
    case _ => getFactory(protocolName)
  }

  def getWriterFactory(protocolName: String): TProtocolFactory = protocolName match {
    case KnownTProtocolNames.TBSONProtocol => new TBSONProtocol.WriterFactory()
    // just use the DBObject based writer protocol
    case KnownTProtocolNames.TBSONBinaryProtocol => new TBSONProtocol.WriterFactory()
    case _ => getFactory(protocolName)
  }

  private def getFactory(protocolName: String): TProtocolFactory = protocolName match {
    case KnownTProtocolNames.TBinaryProtocol => new TBinaryProtocol.Factory()
    case KnownTProtocolNames.TCompactProtocol => new TCompactProtocol.Factory()
    case KnownTProtocolNames.TJSONProtocol => new TJSONProtocol.Factory()
    case KnownTProtocolNames.TReadableJSONProtocol => new TReadableJSONProtocol.Factory()
    case _ => throw new Exception("Unrecognized protocol: %s".format(protocolName))
  }
}

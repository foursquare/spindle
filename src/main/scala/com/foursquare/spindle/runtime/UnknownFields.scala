// Copyright 2014 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.runtime

import org.apache.thrift.TBase
import org.apache.thrift.protocol.{TField, TProtocol}


// A single unknown field, as read from the wire.
case class UnknownField(tfield: TField, value: UValue) {
  // Must override because TField's equals method has the wrong signature. Sigh.
  override def equals(other: Any) = other match {
    case o: UnknownField => tfield.name == o.tfield.name &&
                            tfield.id == o.tfield.id &&
                            tfield.`type` == o.tfield.`type` &&
                            value == o.value
    case _ => false
  }
}


// Unknown fields, encountered on the wire during deserialization from the specified protocol.
// We stash that data away in this hidden data structure inside the record, so we can serialize it out
// again later. This allows us to roundtrip through an older version of a record without data loss.
// Unknown fields may be serialized either inline or in a blob, depending on whether we have
// enough information to do so. See comments on UnknownFieldsBlob for details.
//
// - inputProtocolName: The protocol the unknown fields are being read from.
case class UnknownFields(inputProtocolName: String) {
  // Unknown fields are stashed here until we need to write them out.
  private var stash: List[UnknownField] = Nil

  def write(oprot: TProtocol) {
    val outputProtocolName = TProtocolInfo.getProtocolName(oprot)
    // We can write inline if both protocols are robust, or if they are the same protocol.
    if (outputProtocolName == inputProtocolName ||
        TProtocolInfo.isRobust(inputProtocolName) && TProtocolInfo.isRobust(outputProtocolName)) {
      writeInline(oprot)
    } else {  // Write as a blob.
      val blob = UnknownFieldsBlob.toBlob(this)
      blob.write(oprot)
    }
  }

  // Read a field whose wire name/id is unknown to rec.
  def readUnknownField(iprot: TProtocol, wireTField: TField, rec: TBase[_, _]) {
    if (wireTField.id == UnknownFieldsBlob.magicField.id || wireTField.name == UnknownFieldsBlob.magicField.name) {
      val blob = UnknownFieldsBlob.fromMagicField(iprot)
      blob.read(rec)
    } else {
      // This is a regular, inline field that rec doesn't know about, so stash it.
      readInline(iprot, wireTField)
    }
  }

  def writeInline(oprot: TProtocol) {
    stash.reverse foreach {
      field: UnknownField => {
        oprot.writeFieldBegin(field.tfield)
        field.value.write(oprot)
        oprot.writeFieldEnd()
      }
    }
  }

  def readInline(iprot: TProtocol, tfield: TField) {
    val fieldVal: UValue = UValue.read(iprot, tfield.`type`)
    this.stash = UnknownField(tfield, fieldVal) :: this.stash
  }
}

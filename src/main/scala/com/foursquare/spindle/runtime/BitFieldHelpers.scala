// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

object BitFieldHelpers {
  def getValue(bitfield: Int, bitIndex: Int): Boolean = {
    /** We only allow 16 values because half the bits are used for
      * "isSet" semantics.
      */
    assert(bitIndex < 16)
    ((1 << bitIndex) & bitfield) != 0
  }

  def getIsSet(bitfield: Int, bitIndex: Int): Boolean = {
    /** We only allow 16 values because half the bits are used for
      * "isSet" semantics.
      */
    assert(bitIndex < 16)
    ((1 << (bitIndex + 16)) & bitfield) != 0
  }

  def bitFieldToStruct(bitfield: Int, meta: MetaRecord[_]): meta.Trait = {
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.unsafeManifest == manifest[Boolean]))
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.id >= 1 && f.id <= 16))
    assert(meta.fields.size <= 16)

    val result = meta.createRawRecord

    meta.fields.foreach((field: FieldDescriptor[_, _, meta.type]) => {
      val setter = field.setterRaw.asInstanceOf[(meta.type#Raw, Boolean) => Unit]
      if (getIsSet(bitfield, field.id - 1)) {
        setter(result, getValue(bitfield, field.id - 1))
      }
    })

    result
  }

  def getLongValue(bitfield: Long, bitIndex: Int): Boolean = {
    /** We only allow 31 values because half the bits are used for
      * "isSet" semantics and one bit is reserved for a migration flag.
      */
    assert(bitIndex < 31)
    ((1L << bitIndex) & bitfield) != 0
  }

  def getLongIsSet(bitfield: Long, bitIndex: Int): Boolean = {
    /** We only allow 31 values because half the bits are used for
      * "isSet" semantics and one bit is reserved for a migration flag.
      */
    assert(bitIndex < 31)
    ((1L << (bitIndex + 32)) & bitfield) != 0
  }

  def longBitFieldToStruct(bitfield: Long, meta: MetaRecord[_]): meta.Trait = {
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.unsafeManifest == manifest[Boolean]))
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.id >= 1 && f.id <= 31))
    assert(meta.fields.size <= 31)

    val result = meta.createRawRecord

    meta.fields.foreach((field: FieldDescriptor[_, _, meta.type]) => {
      val setter = field.setterRaw.asInstanceOf[(meta.type#Raw, Boolean) => Unit]
      if (getLongIsSet(bitfield, field.id - 1)) {
        setter(result, getLongValue(bitfield, field.id - 1))
      }
    })

    result
  }

  def getValueNoSetBits(bitfield: Int, bitIndex: Int): Boolean = {
    assert(bitIndex < 32)
    ((1 << bitIndex) & bitfield) != 0
  }

  def bitFieldToStructNoSetBits(bitfield: Int, meta: MetaRecord[_]): meta.Trait = {
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.unsafeManifest == manifest[Boolean]))
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.id >= 1 && f.id <= 32))
    assert(meta.fields.size <= 32)

    val result = meta.createRawRecord

    meta.fields.foreach((field: FieldDescriptor[_, _, meta.type]) => {
      val setter = field.setterRaw.asInstanceOf[(meta.type#Raw, Boolean) => Unit]
      setter(result, getValueNoSetBits(bitfield, field.id - 1))
    })

    result
  }

  def getLongValueNoSetBits(bitfield: Long, bitIndex: Int): Boolean = {
    assert(bitIndex < 64)
    ((1L << bitIndex) & bitfield) != 0
  }

  def longBitFieldToStructNoSetBits(bitfield: Long, meta: MetaRecord[_]): meta.Trait = {
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.unsafeManifest == manifest[Boolean]))
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.id >= 1 && f.id <= 64))
    assert(meta.fields.size <= 64)

    val result = meta.createRawRecord

    meta.fields.foreach((field: FieldDescriptor[_, _, meta.type]) => {
      val setter = field.setterRaw.asInstanceOf[(meta.type#Raw, Boolean) => Unit]
      setter(result, getLongValueNoSetBits(bitfield, field.id - 1))
    })

    result
  }
}

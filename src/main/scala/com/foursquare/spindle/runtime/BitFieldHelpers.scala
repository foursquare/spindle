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
    verifyMeta(meta, 16)

    val result = meta.createRawRecord

    meta.fields.foreach((field: FieldDescriptor[_, _, meta.type]) => {
      val setter = field.setterRaw.asInstanceOf[(meta.type#Raw, Boolean) => Unit]
      if (getIsSet(bitfield, field.id - 1)) {
        setter(result, getValue(bitfield, field.id - 1))
      }
    })

    result
  }

  def structToBitField(struct: Record[_]): Int = {
    val meta = struct.meta
    verifyMeta(meta, 16)
    meta.fields.map((field: FieldDescriptor[_, _, meta.type]) => {
      val getter = field.getter.asInstanceOf[struct.type => Option[Boolean]]
      val bitIndex = (field.id - 1)
      val valueOpt = getter(struct)
      val (v, isSet) = valueOpt match {
        case Some(v) => (if (v) 1 else 0, 1)
        case _ => (0, 0)
      }
      val isSetMask: Int = (isSet << (bitIndex + 16))
      val valueMask: Int = (v << bitIndex)
      isSetMask | valueMask
    }).foldLeft(0)(_ | _)
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
    verifyMeta(meta, 31)

    val result = meta.createRawRecord

    meta.fields.foreach((field: FieldDescriptor[_, _, meta.type]) => {
      val setter = field.setterRaw.asInstanceOf[(meta.type#Raw, Boolean) => Unit]
      if (getLongIsSet(bitfield, field.id - 1)) {
        setter(result, getLongValue(bitfield, field.id - 1))
      }
    })

    result
  }

  def structToLongBitField(struct: Record[_]): Long = {
    val meta = struct.meta
    verifyMeta(meta, 31)
    meta.fields.map((field: FieldDescriptor[_, _, meta.type]) => {
      val getter = field.getter.asInstanceOf[struct.type => Option[Boolean]]
      val bitIndex = (field.id - 1)
      val valueOpt = getter(struct)
      val (v, isSet) = valueOpt match {
        case Some(v) => (if (v) 1L else 0L, 1L)
        case _ => (0L, 0L)
      }
      val isSetMask = (isSet << (bitIndex + 32))
      val valueMask = (v << bitIndex)
      isSetMask | valueMask
    }).foldLeft(0L)(_ | _)
  }

  def getValueNoSetBits(bitfield: Int, bitIndex: Int): Boolean = {
    assert(bitIndex < 32)
    ((1 << bitIndex) & bitfield) != 0
  }

  def bitFieldToStructNoSetBits(bitfield: Int, meta: MetaRecord[_]): meta.Trait = {
    verifyMeta(meta, 32)

    val result = meta.createRawRecord

    meta.fields.foreach((field: FieldDescriptor[_, _, meta.type]) => {
      val setter = field.setterRaw.asInstanceOf[(meta.type#Raw, Boolean) => Unit]
      setter(result, getValueNoSetBits(bitfield, field.id - 1))
    })

    result
  }

  def structToBitFieldNoSetBits(struct: Record[_]): Int = {
    val meta = struct.meta
    verifyMeta(meta, 32)
    meta.fields.map((field: FieldDescriptor[_, _, meta.type]) => {
      val getter = field.getter.asInstanceOf[struct.type => Option[Boolean]]
      val bitIndex = (field.id - 1)
      val value = getter(struct) match {
        case Some(true) => 1
        case _ => 0
      }
      val valueMask: Int = (value << bitIndex)
      valueMask
    }).foldLeft(0)(_ | _)
  }

  def getLongValueNoSetBits(bitfield: Long, bitIndex: Int): Boolean = {
    assert(bitIndex < 64)
    ((1L << bitIndex) & bitfield) != 0
  }

  def longBitFieldToStructNoSetBits(bitfield: Long, meta: MetaRecord[_]): meta.Trait = {
    verifyMeta(meta, 64)

    val result = meta.createRawRecord

    meta.fields.foreach((field: FieldDescriptor[_, _, meta.type]) => {
      val setter = field.setterRaw.asInstanceOf[(meta.type#Raw, Boolean) => Unit]
      setter(result, getLongValueNoSetBits(bitfield, field.id - 1))
    })

    result
  }

  def structToLongBitFieldNoSetBits(struct: Record[_]): Long = {
    val meta = struct.meta
    verifyMeta(meta, 64)
    meta.fields.map((field: FieldDescriptor[_, _, meta.type]) => {
      val getter = field.getter.asInstanceOf[struct.type => Option[Boolean]]
      val bitIndex = (field.id - 1)
      val value = getter(struct) match {
        case Some(true) => 1L
        case _ => 0L
      }
      val valueMask = (value << bitIndex)
      valueMask
    }).foldLeft(0L)(_ | _)
  }

  private def verifyMeta(meta: MetaRecord[_], availableBits: Int) = {
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.unsafeManifest == manifest[Boolean]))
    assert(meta.fields.forall((f: UntypedFieldDescriptor) => f.id >= 1 && f.id <= availableBits))
    assert(meta.fields.size <= availableBits)
  }
}

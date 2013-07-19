// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.recordv2

import java.nio.ByteBuffer
import org.apache.thrift.{TBase, TException}
import org.apache.thrift.protocol.{TField, TList, TMap, TProtocol, TSet, TStruct, TType => JavaTType}

object TTypeIntrepreter {

  val BooleanClass = classOf[Boolean]
  val ByteClass = classOf[Byte]
  val DoubleClass = classOf[Double]
  val ShortClass = classOf[Short]
  val IntClass = classOf[Int]
  val LongClass = classOf[Long]
  val StringClass = classOf[String]
  val MapClass = classOf[Map[_, _]]
  val SetClass = classOf[Set[_]]
  val SeqClass = classOf[Seq[_]]
  val EnumClass = classOf[Enum[_]]
  val ByteBufferClass = classOf[ByteBuffer]

  def apply(m: Manifest[_]): (Byte, (TProtocol, Any) => Unit) = {
    m.erasure match {
      case BooleanClass => (
        JavaTType.BOOL,
        (o: TProtocol, x: Any) => o.writeBool(x.asInstanceOf[Boolean])
      )
      case ByteClass => (
        JavaTType.BYTE,
        (o: TProtocol, x: Any) => o.writeByte(x.asInstanceOf[Byte])
      )
      case DoubleClass => (
        JavaTType.DOUBLE,
        (o: TProtocol, x: Any) => o.writeDouble(x.asInstanceOf[Double])
      )
      case ShortClass => (
        JavaTType.I16,
        (o: TProtocol, x: Any) => o.writeI16(x.asInstanceOf[Short])
      )
      case IntClass => (
        JavaTType.I32,
        (o: TProtocol, x: Any) => o.writeI32(x.asInstanceOf[Int])
      )
      case LongClass => (
        JavaTType.I64,
        (o: TProtocol, x: Any) => o.writeI64(x.asInstanceOf[Long])
      )
      case StringClass => (
        JavaTType.STRING,
        (o: TProtocol, x: Any) => o.writeString(x.asInstanceOf[String])
      )
      case ByteBufferClass => (
        JavaTType.STRING,
        (o: TProtocol, x: Any) => o.writeBinary(x.asInstanceOf[ByteBuffer])
      )
      case SetClass => {
        val (ttype, fn) = TTypeIntrepreter(m.typeArguments(0))
        (JavaTType.SET,
        (o: TProtocol, x: Any) => {
          val xs = x.asInstanceOf[Set[_]]
          o.writeSetBegin(new TSet(ttype, xs.size))
          xs.foreach(fn(o, _))
          o.writeSetEnd
        })
      }
      case SeqClass => {
        val (ttype, fn) = TTypeIntrepreter(m.typeArguments(0))
        (JavaTType.LIST,
        (o: TProtocol, x: Any) => {
          val xs = x.asInstanceOf[Seq[_]]
          o.writeListBegin(new TList(ttype, xs.size))
          xs.foreach(fn(o, _))
          o.writeListEnd
        })
      }
      case MapClass => {
        val (ttypeKey, fnKey) = TTypeIntrepreter(m.typeArguments(0))
        val (ttypeValue, fnValue) = TTypeIntrepreter(m.typeArguments(1))
        (JavaTType.MAP,
        (o: TProtocol, x: Any) => {
          val xs = x.asInstanceOf[Map[_, _]]
          o.writeMapBegin(new TMap(ttypeKey, ttypeValue, xs.size))
          xs.foreach(x => {fnKey(o, x._1); fnValue(o, x._2)} )
          o.writeSetEnd
        })
      }
      case _ if manifest[Enum[_]].erasure.isAssignableFrom(m.erasure) => (
        JavaTType.ENUM,
        (o: TProtocol, x: Any) => {
          o.writeString(x.asInstanceOf[Enum[_]].name)
        }
      )
      case _ if manifest[TBase[_, _]].erasure.isAssignableFrom(m.erasure) => (
        JavaTType.STRUCT,
        (o: TProtocol, x: Any) => {
          val tbase = x.asInstanceOf[TBase[_, _] with Record[_]]
          new LongNameRecordProxy(tbase).write(o)
        }
      )
      // TODO(dan): ObjectId, DateTime and all the other enhanced fields.
      case _ => (
        JavaTType.VOID,
        (o: TProtocol, x: Any) => throw new TException("Unsupported type: " + m.erasure.getName)
      )
    }
  }
}

class LongNameRecordProxy(t: TBase[_, _] with Record[_]) {
  val structDesc = new TStruct(t.meta.recordName)

  def write(oprot: TProtocol): Unit = {
    oprot.writeStructBegin(structDesc)
    val fields: Seq[UntypedFieldDescriptor] = t.meta.fields
    fields.foreach(field => {
      field.unsafeGetterOption(t).foreach(value => {
        val (fieldType, writeFieldValue) = TTypeIntrepreter(field.unsafeManifest)
        oprot.writeFieldBegin(new TField(field.longName, fieldType, field.id.toShort))
        writeFieldValue(oprot, value)
        oprot.writeFieldEnd
      })
    })
    oprot.writeFieldStop
    oprot.writeStructEnd
  }

  def read(oprot: TProtocol): Unit = throw new TException("Not implemented")
}
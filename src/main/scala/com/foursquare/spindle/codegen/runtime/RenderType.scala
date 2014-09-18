// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.runtime

import scalaj.collection.Imports._

class NotImplementedException(s: String) extends Exception(s)

trait RenderType {
  def text: String
  def boxedText: String
  def defaultText: String
  def compareTemplate: String
  def fieldDefTemplate: String
  def fieldImplTemplate: String
  def fieldProxyTemplate: String
  def fieldMutableTemplate: String // = "field/mutable.ssp"
  def fieldMutableProxyTemplate: String // = "field/mutableproxy.ssp"
  def fieldWriteTemplate: String
  def fieldReadTemplate: String
  def underlying: RenderType = this
  def ttype: TType
  def isEnhanced: Boolean = false
  def isNullable: Boolean = false
  def isContainer: Boolean = false
  def isEnum: Boolean = false
  def usesSetVar: Boolean
  def hasOrdering: Boolean
  def hasCompare: Boolean = true
  def renderValueSupported = false
  def renderValue(v: String): Option[String] = None
}

case class PrimitiveRenderType(
    override val text: String,
    override val boxedText: String,
    override val defaultText: String,
    override val ttype: TType
) extends RenderType {
  val tprotocolSuffix = ttype match {
    case TType.BOOL => "Bool"
    case TType.BYTE => "Byte"
    case TType.DOUBLE => "Double"
    case TType.ENUM => "I32"
    case TType.I16 => "I16"
    case TType.I32 => "I32"
    case TType.I64 => "I64"
    case _ => throw new IllegalArgumentException("Unrecognized protocol suffix for ttype " + ttype)
  }

  override def compareTemplate = "compare/primitive.ssp"
  override def fieldDefTemplate: String = "field/def_primitive.ssp"
  override def fieldImplTemplate: String = "field/impl_primitive.ssp"
  override def fieldProxyTemplate: String = "field/proxy_primitive.ssp"
  override def fieldMutableTemplate: String = "field/mutable.ssp"
  override def fieldMutableProxyTemplate: String = "field/mutableproxy.ssp"
  override def fieldWriteTemplate: String = "write/primitive.ssp"
  override def fieldReadTemplate: String = "read/primitive.ssp"
  override def usesSetVar: Boolean = true
  override def hasOrdering: Boolean = true
  override val renderValueSupported = true
  override def renderValue(v: String) = Some(v)
}

trait RefRenderType extends RenderType {
  override def boxedText: String = text
  override def defaultText: String = "null"
  override def compareTemplate = "compare/ref.ssp"
  override def fieldDefTemplate: String = "field/def_ref.ssp"
  override def fieldImplTemplate: String = "field/impl_ref.ssp"
  override def fieldProxyTemplate: String = "field/proxy_ref.ssp"
  override def fieldMutableTemplate: String = "field/mutable.ssp"
  override def fieldMutableProxyTemplate: String = "field/mutableproxy.ssp"
  override def isNullable: Boolean = true
  override def usesSetVar: Boolean = false
}

trait EnhancedRenderType extends RenderType {
  override def isEnhanced: Boolean = true
}

case object StringRenderType extends RefRenderType {
  override def text: String = "String"
  override def ttype: TType = TType.STRING
  override def fieldWriteTemplate: String = "write/string.ssp"
  override def fieldReadTemplate: String = "read/string.ssp"
  override def hasOrdering: Boolean = true
  override val renderValueSupported = true
  override def renderValue(v: String) = Some(v)
}

case object BinaryRenderType extends RefRenderType {
  override def text: String = "java.nio.ByteBuffer"
  override def ttype: TType = TType.STRING
  override def fieldDefTemplate: String = "field/def_binary.ssp"
  override def fieldImplTemplate: String = "field/impl_binary.ssp"
  override def fieldProxyTemplate: String = "field/proxy_binary.ssp"
  override def fieldMutableTemplate: String = "field/mutable_binary.ssp"
  override def fieldMutableProxyTemplate: String = "field/mutableproxy_binary.ssp"
  override def fieldWriteTemplate: String = "write/binary.ssp"
  override def fieldReadTemplate: String = "read/binary.ssp"
  override def hasOrdering: Boolean = false
}

case class EnumRenderType(override val text: String) extends RefRenderType {
  override def ttype: TType = TType.I32
  override def fieldWriteTemplate: String = "write/enum.ssp"
  override def fieldReadTemplate: String = "read/enum.ssp"
  override def isEnum: Boolean = true
  override def hasOrdering: Boolean = false
  override def renderValueSupported = true
  override def renderValue(v: String) = Some(v)
}

case class StructRenderType(override val text: String) extends RefRenderType {
  override def fieldWriteTemplate: String = "write/struct.ssp"
  override def fieldReadTemplate: String = "read/struct.ssp"
  override def ttype: TType = TType.STRUCT
  override def hasOrdering: Boolean = false
}

case class ThriftJsonRenderType(ref: RenderType) extends RefRenderType with EnhancedRenderType {
  override def text: String = "net.liftweb.json.JObject"
  override def fieldWriteTemplate: String = "write/json.ssp"
  override def fieldReadTemplate: String = "read/json.ssp"
  override def compareTemplate = "compare/json.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = TType.STRING
  override def hasOrdering: Boolean = false
}

trait ContainerRenderType extends RefRenderType {
  def container: String
  override def fieldDefTemplate: String = "field/def_container.ssp"
  override def fieldImplTemplate: String = "field/impl_container.ssp"
  override def fieldProxyTemplate: String = "field/proxy_container.ssp"
  override def isContainer: Boolean = true
  override def hasOrdering: Boolean = false
}

abstract class Container1RenderType(override val container: String, val elem: RenderType) extends ContainerRenderType {
  override def text: String = "%s[%s]".format(container, elem.text)
}

// TODO(jorge): Make this immutable.Seq
case class SeqRenderType(e1: RenderType) extends Container1RenderType("scala.collection.Seq", e1) {
  override def ttype: TType = TType.LIST
  override def compareTemplate: String = "compare/seq.ssp"
  override def fieldWriteTemplate: String = "write/seq.ssp"
  override def fieldReadTemplate: String = "read/seq.ssp"
  override def underlying: SeqRenderType = SeqRenderType(e1.underlying)
  override val renderValueSupported = e1.renderValueSupported
  override def renderValue(v: String) = try {
    if (renderValueSupported) {
      val to = v.size-2
      val withoutParens = v.slice(1, v.size-1)
      val values = withoutParens.split("\\s*,\\s*")
      Some("List(%s)".format(values.flatMap(v => e1.renderValue(v)).mkString(", ")))
    } else None

  } catch {
    case e: Exception => {
      throw new Exception("unable to parse map value '%s'".format())
    }
  }
}

case class SetRenderType(e1: RenderType) extends Container1RenderType("scala.collection.immutable.Set", e1) {
  override def ttype: TType = TType.SET
  override def compareTemplate: String = "compare/set.ssp"
  override def fieldWriteTemplate: String = "write/set.ssp"
  override def fieldReadTemplate: String = "read/set.ssp"
  override def underlying: SetRenderType = SetRenderType(e1.underlying)
}

abstract class Container2RenderType(override val container: String, val elem1: RenderType, val elem2: RenderType) extends ContainerRenderType {
  override def text: String = "%s[%s, %s]".format(container, elem1.text, elem2.text)
}

case class MapRenderType(e1: RenderType, e2: RenderType) extends Container2RenderType("scala.collection.immutable.Map", e1, e2) {
  override def ttype: TType = TType.MAP
  override def compareTemplate: String = "compare/map.ssp"
  override def fieldWriteTemplate: String = "write/map.ssp"
  override def fieldReadTemplate: String = "read/map.ssp"
  override def underlying: MapRenderType = MapRenderType(e1.underlying, e2.underlying)
  override val renderValueSupported = e1.renderValueSupported && e2.renderValueSupported
  override def renderValue(v: String) = try {
    if (renderValueSupported) {
      val withoutParens = v.slice(1, v.size-1)
      val tuples = withoutParens.split("\\s*,\\s*").map(_.split("\\s*:\\s*"))
      val formatted = tuples.flatMap{case Array(k,v) =>
        (e1.renderValue(k), e2.renderValue(v)) match {
          case (Some(rk), Some(rv)) => Some("%s->%s".format(rk,rv))
          case _ => None
        }
      }
      Some("Map(%s)".format(formatted.mkString(", ")))
    } else None

  } catch {
    case e: Exception => {
      throw new Exception("unable to parse map value '%s'".format(v), e)
    }
  }

}

case class TypedefRenderType(override val text: String, ref: RenderType) extends RenderType {
  override def boxedText: String = ref.boxedText
  override def defaultText: String = ref.defaultText
  override def fieldDefTemplate: String = ref.fieldDefTemplate
  override def fieldImplTemplate: String = ref.fieldImplTemplate
  override def fieldProxyTemplate: String = ref.fieldProxyTemplate
  override def compareTemplate: String = ref.compareTemplate
  override def fieldMutableTemplate: String = ref.fieldMutableTemplate
  override def fieldMutableProxyTemplate: String = ref.fieldMutableProxyTemplate
  override def fieldWriteTemplate: String = "write/typedef.ssp"
  override def fieldReadTemplate: String = "read/typedef.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = ref.ttype
  override def isEnhanced: Boolean = ref.isEnhanced
  override def isNullable: Boolean = ref.isNullable
  override def isContainer: Boolean = ref.isContainer
  override def isEnum: Boolean = ref.isEnum
  override def usesSetVar: Boolean = ref.usesSetVar
  override def hasOrdering: Boolean = ref.hasOrdering
  override def renderValueSupported = ref.renderValueSupported
  override def renderValue(v: String) = ref.renderValue(v)
}

case class NewtypeRenderType(override val text: String, ref: RenderType) extends RenderType {
  override def boxedText: String = text
  override def defaultText: String = text + "(" + ref.defaultText + ")"
  override def fieldDefTemplate: String = ref.fieldDefTemplate
  override def fieldImplTemplate: String = ref.fieldImplTemplate
  override def fieldProxyTemplate: String = ref.fieldProxyTemplate
  override def compareTemplate: String = ref.compareTemplate
  override def fieldMutableTemplate: String = ref.fieldMutableTemplate
  override def fieldMutableProxyTemplate: String = ref.fieldMutableProxyTemplate
  override def fieldWriteTemplate: String = "write/newtype.ssp"
  override def fieldReadTemplate: String = "read/newtype.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = ref.ttype
  override def isEnhanced: Boolean = ref.isEnhanced
  override def isNullable: Boolean = ref.isNullable
  override def isContainer: Boolean = ref.isContainer
  override def isEnum: Boolean = ref.isEnum
  override def usesSetVar: Boolean = ref.usesSetVar
  override def hasOrdering: Boolean = ref.hasOrdering
  override def renderValueSupported = ref.renderValueSupported
  override def renderValue(v: String) = ref.renderValue(v).map(rv => text + "(" + rv + ")")
}

case class ObjectIdRenderType(ref: RenderType) extends RefRenderType with EnhancedRenderType {
  override def text: String = "org.bson.types.ObjectId"
  override def fieldWriteTemplate: String = "write/objectid.ssp"
  override def fieldReadTemplate: String = "read/objectid.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = TType.STRING
  override def hasOrdering: Boolean = true
  override def renderValueSupported = true
  override def renderValue(v: String) = Some("new org.bson.types.ObjectId(%s)".format(v))
}

case class BSONObjectRenderType(ref: RenderType) extends RefRenderType with EnhancedRenderType {
  override def text: String = "org.bson.BSONObject"
  override def fieldWriteTemplate: String = "write/bsonobject.ssp"
  override def fieldReadTemplate: String = "read/bsonobject.ssp"
  // override def compareTemplate = "compare/bsonobject.ssp"
  override def hasCompare: Boolean = false
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = TType.STRING
  override def hasOrdering: Boolean = false
  override def renderValueSupported = false
  override def renderValue(v: String) = None //Some("new org.bson.types.ObjectId(%s)".format(v))
}

case class DateTimeRenderType(ref: RenderType) extends RefRenderType with EnhancedRenderType {
  override def text: String = "org.joda.time.DateTime"
  override def fieldWriteTemplate: String = "write/datetime.ssp"
  override def fieldReadTemplate: String = "read/datetime.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = TType.I64
  override def hasOrdering: Boolean = false

}

case class JavaDateRenderType(ref: RenderType) extends RefRenderType with EnhancedRenderType {
  override def text: String = "java.util.Date"
  override def fieldWriteTemplate: String = "write/javadate.ssp"
  override def fieldReadTemplate: String = "read/javadate.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = TType.STRING
  override def hasOrdering: Boolean = false
}

case class DollarAmountRenderType(ref: RenderType) extends RefRenderType with EnhancedRenderType {
  override def text: String = "com.foursquare.common.base.DollarAmount"
  override def fieldWriteTemplate: String = "write/dollaramount.ssp"
  override def fieldReadTemplate: String = "read/dollaramount.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = TType.I64
  override def hasOrdering: Boolean = false
}

case class MessageSetRenderType(ref: RenderType) extends RefRenderType with EnhancedRenderType {
  override def text: String = "com.foursquare.common.types.MessageSet"
  override def defaultText: String = "com.foursquare.common.types.MessageSet.Empty"
  override def fieldWriteTemplate: String = "write/messageset.ssp"
  override def fieldReadTemplate: String = "read/messageset.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = TType.STRUCT
  override def hasOrdering: Boolean = false
}

case class TypesafeIdRenderType(className: String, ref: RenderType) extends RefRenderType with EnhancedRenderType {
  override def defaultText: String = className + ".Id(" + ref.defaultText + ")"
  override def text: String = className + ".Id"
  override def compareTemplate: String = ref.compareTemplate
  override def fieldWriteTemplate: String = "write/id.ssp"
  override def fieldReadTemplate: String = "read/id.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = ref.ttype
  override def usesSetVar: Boolean = ref.usesSetVar
  override def hasOrdering: Boolean = ref.hasOrdering
}

case class BitfieldStructRenderType(
    className: String,
    ref: RenderType,
    hasSetBits: Boolean
) extends RenderType {
  override def boxedText: String = ref.boxedText
  override def text: String = ref.text
  override def defaultText: String = ref.defaultText
  override def compareTemplate: String = "compare/primitive.ssp"
  override def fieldDefTemplate: String = "field/def_bitfield.ssp"
  override def fieldImplTemplate: String = "field/impl_bitfield.ssp"
  override def fieldProxyTemplate: String = "field/proxy_primitive.ssp"
  override def fieldMutableTemplate: String = "field/mutable.ssp"
  override def fieldMutableProxyTemplate: String = "field/mutableproxy.ssp"
  override def fieldWriteTemplate: String = "write/bitfield.ssp"
  override def fieldReadTemplate: String = "read/bitfield.ssp"
  override def underlying: RenderType = ref.underlying
  override def ttype: TType = ref.ttype
  override def isEnhanced: Boolean = false
  override def usesSetVar: Boolean = true
  override def hasOrdering: Boolean = false

  val bitfieldRead = (hasSetBits, ref.ttype) match {
    case (true, TType.I32) => "com.foursquare.spindle.BitFieldHelpers.bitFieldToStruct"
    case (true, TType.I64) => "com.foursquare.spindle.BitFieldHelpers.longBitFieldToStruct"
    case (false, TType.I32) => "com.foursquare.spindle.BitFieldHelpers.bitFieldToStructNoSetBits"
    case (false, TType.I64) => "com.foursquare.spindle.BitFieldHelpers.longBitFieldToStructNoSetBits"
    case _ => throw new IllegalArgumentException("Unsupported bitfield type: " + ref.ttype + " with hasSetBits: " + hasSetBits)
  }

  val bitfieldWrite = (hasSetBits, ref.ttype) match {
    case (true, TType.I32) => "com.foursquare.spindle.BitFieldHelpers.structToBitField"
    case (true, TType.I64) => "com.foursquare.spindle.BitFieldHelpers.structToLongBitField"
    case (false, TType.I32) => "com.foursquare.spindle.BitFieldHelpers.structToBitFieldNoSetBits"
    case (false, TType.I64) => "com.foursquare.spindle.BitFieldHelpers.structToLongBitFieldNoSetBits"
    case _ => throw new IllegalArgumentException("Unsupported bitfield type: " + ref.ttype + " with hasSetBits: " + hasSetBits)
  }
}

object RenderType {

  // Allow fs:JsonX so we can add more meta-data to the type for javascript codegen.
  val JsonEnhancedType = """fs:Json(.*)""".r

  def apply(tpe: TypeReference): RenderType = {
    tpe match {
      case BoolRef => PrimitiveRenderType("Boolean", "java.lang.Boolean", "false", TType.BOOL)
      case ByteRef => PrimitiveRenderType("Byte", "java.lang.Byte", "0", TType.BYTE)
      case I16Ref => PrimitiveRenderType("Short", "java.lang.Short", "0", TType.I16)
      case I32Ref => PrimitiveRenderType("Int", "java.lang.Integer", "0", TType.I32)
      case I64Ref => PrimitiveRenderType("Long", "java.lang.Long", "0L", TType.I64)
      case DoubleRef => PrimitiveRenderType("Double", "java.lang.Double", "0.0", TType.DOUBLE)
      case StringRef => StringRenderType
      case BinaryRef => BinaryRenderType
      case ListRef(elem) => SeqRenderType(RenderType(elem))
      case SetRef(elem) => SetRenderType(RenderType(elem))
      case MapRef(key, value) => MapRenderType(RenderType(key), RenderType(value))
      case EnumRef(name) => EnumRenderType(name)
      case StructRef(name) => StructRenderType(name)
      case UnionRef(name) => StructRenderType(name)
      case ExceptionRef(name) => StructRenderType(name)
      case ServiceRef(name) => throw new CodegenException("Trying to render unrenderable Service type: " + name)
      case TypedefRef(name, ref) => TypedefRenderType(name, RenderType(ref))
      case NewtypeRef(name, ref) => NewtypeRenderType(name, RenderType(ref))
      case EnhancedTypeRef(name, TypedefRef(_, ref)) => RenderType(EnhancedTypeRef(name, ref))
      case EnhancedTypeRef("bson:ObjectId", ref @ BinaryRef) => ObjectIdRenderType(RenderType(ref))
      case EnhancedTypeRef("bson:BSONObject", ref @ BinaryRef) => BSONObjectRenderType(RenderType(ref))
      case EnhancedTypeRef("bson:DateTime", ref @ I64Ref) => DateTimeRenderType(RenderType(ref))
      case EnhancedTypeRef("java:Date", ref @ StringRef) => JavaDateRenderType(RenderType(ref))
      case EnhancedTypeRef("fs:DollarAmount", ref @ I64Ref) => DollarAmountRenderType(RenderType(ref))
      case EnhancedTypeRef(JsonEnhancedType(suffix), ref @ StringRef) => ThriftJsonRenderType(RenderType(ref))
      case EnhancedTypeRef("fs:MessageSet", ref: StructRef) => MessageSetRenderType(RenderType(ref))
      case EnhancedTypeRef(name, _) => throw new CodegenException("Unknown enhanced type: " + name)
      case BitfieldRef(name, bitType, hasSetBits) => BitfieldStructRenderType(name, RenderType(bitType), hasSetBits)
    }
  }
}

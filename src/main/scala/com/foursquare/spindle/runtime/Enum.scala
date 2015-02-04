// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

abstract class Enum[T <: Enum[T]] extends Ordered[T] { self: T =>
  def meta: EnumMeta[T]

  def id: Int
  def name: String
  def stringValue: String

  // Implementation of Ordered[T].
  override def compare(other: T) = this.id.compare(other.id)

  def annotations: Annotations = Annotations.empty
}

abstract class EnumMeta[T <: Enum[T]] {
  def values: Vector[T]

  def findByIdOrNull(id: Int): T
  def findByNameOrNull(name: String): T

  def findByIdOrUnknown(id: Int): T

  def findById(id: Int): Option[T] = Option(findByIdOrNull(id))
  def findByName(name: String): Option[T] = Option(findByNameOrNull(name))

  def apply(id: Int): Option[T] = findById(id)

  // This is useful if we want to pattern match strings into instances of the enumeration.
  def unapply(name: String): Option[T] = findByName(name)

  // Some of our existing Mongo records store enumerations as string values. For backwards compatibility, we support
  // decoding enumerations using those string values. See the string_value annotation on each enumeration value and the
  // with_enum annotation that enhances string fields that should be typed as the enumeration.
  def findByStringValue(v: String): Option[T] = Option(findByStringValueOrNull(v))

  // Implemented by concrete subclasses to implement the conversion.
  def findByStringValueOrNull(v: String): T
  def findByStringValueOrUnknown(v: String): T

  def annotations: Annotations = Annotations.empty
}

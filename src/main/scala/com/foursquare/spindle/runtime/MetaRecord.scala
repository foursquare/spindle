// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

trait UntypedMetaRecord {
  def recordName: String
  def annotations: Annotations
  def createRawRecord: UntypedRecord
  def fields: Seq[UntypedFieldDescriptor]
  def ifInstanceFrom(x: AnyRef): Option[UntypedRecord]
}

trait MetaRecord[R <: Record[R]] extends UntypedMetaRecord {
  type Mutable <: R
  type Raw <: R

  def recordName: String
  def createRawRecord: Raw
  def fields: Seq[FieldDescriptor[_, R, this.type]]
  def ifInstanceFrom(x: AnyRef): Option[R]
}

object MetaRecord {
  def apply[T](implicit c: CompanionProvider[T]): c.CompanionT = c.provide
}

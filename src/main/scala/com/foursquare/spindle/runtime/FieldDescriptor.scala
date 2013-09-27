// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

import com.foursquare.field.{Field, OptionalField}

sealed trait UntypedFieldDescriptor {
  def id: Int
  def name: String
  def longName: String
  def annotations: Map[String, String]
  def unsafeGetterOption: Function1[Any, Option[Any]]
  def unsafeManifest: Manifest[_]
}

trait FieldDescriptor[F, R <: Record[R], M <: MetaRecord[R]] extends Field[F, M] with UntypedFieldDescriptor {
  def id: Int
  def longName: String
  def annotations: Map[String, String]
  def getter: R => Option[F]
  def getterOption: R => Option[F] = getter
  def manifest: Manifest[F]
  def setterRaw: (M#Raw, F) => Unit
  def unsetterRaw: M#Raw => Unit

  override def unsafeGetterOption: Function1[Any, Option[Any]] = getterOption.asInstanceOf[Function1[Any, Option[Any]]]
  override def unsafeManifest: Manifest[_] = manifest
}

trait ForeignKeyField[F, R <: Record[R]] {
  def objSetter: (R, SemitypedHasPrimaryKey[F]) => Unit
  def objGetter: (R, UntypedMetaRecord) => Option[UntypedRecord with SemitypedHasPrimaryKey[F]]
  def alternateObjSetter: (R, AnyRef) => Unit
  def alternateObjGetter: R => Option[AnyRef]
}

trait UntypedBitfieldField {
  def unsafeStructMeta: MetaRecord[_]
}

trait BitfieldField[FR <: Record[FR], FM <: MetaRecord[FR]] extends UntypedBitfieldField {
  def structMeta: FM
  override def unsafeStructMeta: MetaRecord[_] = structMeta
}

case class OptionalFieldDescriptor[F, R <: Record[R], M <: MetaRecord[R]](
    override val name: String,
    override val longName: String,
    override val id: Int,
    override val annotations: Map[String, String],
    override val owner: M,
    override val getter: R => Option[F],
    override val setterRaw: (M#Raw, F) => Unit,
    override val unsetterRaw: M#Raw => Unit,
    override val manifest: Manifest[F]
) extends OptionalField[F, M] with FieldDescriptor[F, R, M]

case class ForeignKeyFieldDescriptor[F, R <: Record[R], M <: MetaRecord[R]](
    override val name: String,
    override val longName: String,
    override val id: Int,
    override val annotations: Map[String, String],
    override val owner: M,
    override val getter: R => Option[F],
    override val setterRaw: (M#Raw, F) => Unit,
    override val unsetterRaw: M#Raw => Unit,
    override val objSetter: (R, SemitypedHasPrimaryKey[F]) => Unit,
    override val objGetter: (R, UntypedMetaRecord) => Option[UntypedRecord with SemitypedHasPrimaryKey[F]],
    override val alternateObjSetter: (R, AnyRef) => Unit,
    override val alternateObjGetter: R => Option[AnyRef],
    override val manifest: Manifest[F]
) extends OptionalField[F, M] with FieldDescriptor[F, R, M] with ForeignKeyField[F, R]

case class BitfieldFieldDescriptor[F, R <: Record[R], M <: MetaRecord[R], FR <: Record[FR], FM <: MetaRecord[FR]](
    override val name: String,
    override val longName: String,
    override val id: Int,
    override val annotations: Map[String, String],
    override val owner: M,
    override val getter: R => Option[F],
    override val setterRaw: (M#Raw, F) => Unit,
    override val unsetterRaw: M#Raw => Unit,
    override val structMeta: FM,
    override val manifest: Manifest[F]
) extends OptionalField[F, M] with FieldDescriptor[F, R, M] with BitfieldField[FR, FM]

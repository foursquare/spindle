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

trait FieldDescriptor[F, R <: Record[R], M <: MetaRecord[R, M]] extends Field[F, M] with UntypedFieldDescriptor {
  def id: Int
  def longName: String
  def annotations: Map[String, String]
  def getter: R => Option[F]
  def getterOption: R => Option[F] = getter
  def manifest: Manifest[F]
  def setterRaw: (MutableRecord[R], F) => Unit
  def unsetterRaw: MutableRecord[R] => Unit

  override def unsafeGetterOption: Function1[Any, Option[Any]] = getterOption.asInstanceOf[Function1[Any, Option[Any]]]
  override def unsafeManifest: Manifest[_] = manifest
}

trait UntypedForeignKeyField {
  def unsafeObjGetter: Function1[Any, Option[Any]]
}

trait ForeignKeyField[F, R <: Record[R]] extends UntypedForeignKeyField {
  def objSetter: (R, SemitypedHasPrimaryKey[F]) => Unit
  def objGetter: (R, UntypedMetaRecord) => Option[UntypedRecord with SemitypedHasPrimaryKey[F]]
  def alternateObjSetter: (R, AnyRef) => Unit
  def alternateObjGetter: R => Option[AnyRef]
}

trait UntypedBitfieldField {
  def unsafeStructMeta: MetaRecord[_, _]
}

trait BitfieldField[FR <: Record[FR], FM <: MetaRecord[FR, FM]] extends UntypedBitfieldField {
  def structMeta: FM
  override def unsafeStructMeta: MetaRecord[_, _] = structMeta
}

trait UntypedStructField {
  def unsafeStructMeta: MetaRecord[_, _]
}

trait StructField[ER <: Record[ER], EM <: MetaRecord[ER, EM]] extends UntypedStructField {
  def structMeta: EM
  override def unsafeStructMeta: MetaRecord[_, _] = structMeta
}

case class OptionalFieldDescriptor[F, R <: Record[R], M <: MetaRecord[R, M]](
    override val name: String,
    override val longName: String,
    override val id: Int,
    override val annotations: Map[String, String],
    override val owner: M,
    override val getter: R => Option[F],
    override val setterRaw: (MutableRecord[R], F) => Unit,
    override val unsetterRaw: MutableRecord[R] => Unit,
    override val manifest: Manifest[F]
) extends OptionalField[F, M] with FieldDescriptor[F, R, M]

case class ForeignKeyFieldDescriptor[F, R <: Record[R], M <: MetaRecord[R, M]](
    override val name: String,
    override val longName: String,
    override val id: Int,
    override val annotations: Map[String, String],
    override val owner: M,
    override val getter: R => Option[F],
    override val setterRaw: (MutableRecord[R], F) => Unit,
    override val unsetterRaw: MutableRecord[R] => Unit,
    override val objSetter: (R, SemitypedHasPrimaryKey[F]) => Unit,
    override val objGetter: (R, UntypedMetaRecord) => Option[UntypedRecord with SemitypedHasPrimaryKey[F]],
    override val unsafeObjGetter: Function1[Any, Option[Any]],
    override val alternateObjSetter: (R, AnyRef) => Unit,
    override val alternateObjGetter: R => Option[AnyRef],
    override val manifest: Manifest[F]
) extends OptionalField[F, M] with FieldDescriptor[F, R, M] with ForeignKeyField[F, R]

case class BitfieldFieldDescriptor[F, R <: Record[R], M <: MetaRecord[R, M], FR <: Record[FR], FM <: MetaRecord[FR, FM]](
    override val name: String,
    override val longName: String,
    override val id: Int,
    override val annotations: Map[String, String],
    override val owner: M,
    override val getter: R => Option[F],
    override val setterRaw: (MutableRecord[R], F) => Unit,
    override val unsetterRaw: MutableRecord[R] => Unit,
    override val structMeta: FM,
    override val manifest: Manifest[F]
) extends OptionalField[F, M] with FieldDescriptor[F, R, M] with BitfieldField[FR, FM]

case class StructFieldDescriptor[R <: Record[R], M <: MetaRecord[R, M], ER <: Record[ER], EM <: MetaRecord[ER, EM]](
    override val name: String,
    override val longName: String,
    override val id: Int,
    override val annotations: Map[String, String],
    override val owner: M,
    override val getter: R => Option[ER],
    override val setterRaw: (MutableRecord[R], ER) => Unit,
    override val unsetterRaw: MutableRecord[R] => Unit,
    override val structMeta: EM,
    override val manifest: Manifest[ER]
) extends OptionalField[ER, M] with FieldDescriptor[ER, R, M] with StructField[ER, EM]

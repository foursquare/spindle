// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

object RuntimeHelpers {
  trait ForeignKeyHooks {
    def missingKey[
        F,
        R <: Record[R],
        M <: MetaRecord[R],
        FR <: Record[FR] with HasPrimaryKey[F, FR]
    ](
        record: R,
        field: ForeignKeyFieldDescriptor[F, R, M],
        foreignMeta: MetaRecord[FR]
    ): Option[FR]

    def missingObj[
        F,
        R <: Record[R],
        M <: MetaRecord[R],
        FR <: Record[FR] with HasPrimaryKey[F, FR]
    ](
        record: R,
        field: ForeignKeyFieldDescriptor[F, R, M],
        foreignMeta: MetaRecord[FR],
        fieldValue: F
    ): Option[FR]

    def mismatchedInstanceType[
        F,
        R <: Record[R],
        M <: MetaRecord[R],
        FR <: Record[FR] with HasPrimaryKey[F, FR]
    ](
        record: R,
        field: ForeignKeyFieldDescriptor[F, R, M],
        foreignMeta: MetaRecord[FR],
        fieldValue: F,
        obj: AnyRef
    ): Option[FR]

    def mismatchedPrimaryKey[
        F,
        R <: Record[R],
        M <: MetaRecord[R],
        FR <: Record[FR] with HasPrimaryKey[F, FR]
    ](
        record: R,
        field: ForeignKeyFieldDescriptor[F, R, M],
        foreignMeta: MetaRecord[FR],
        fieldValue: F,
        foreignRecord: FR
    ): Option[FR]
  }

  object NoopForeignKeyHooks extends ForeignKeyHooks {
    override def missingKey[
        F,
        R <: Record[R],
        M <: MetaRecord[R],
        FR <: Record[FR] with HasPrimaryKey[F, FR]
    ](
        record: R,
        field: ForeignKeyFieldDescriptor[F, R, M],
        foreignMeta: MetaRecord[FR]
    ): Option[FR] = None

    override def missingObj[
        F,
        R <: Record[R],
        M <: MetaRecord[R],
        FR <: Record[FR] with HasPrimaryKey[F, FR]
    ](
        record: R,
        field: ForeignKeyFieldDescriptor[F, R, M],
        foreignMeta: MetaRecord[FR],
        fieldValue: F
    ): Option[FR] = None

    override def mismatchedInstanceType[
        F,
        R <: Record[R],
        M <: MetaRecord[R],
        FR <: Record[FR] with HasPrimaryKey[F, FR]
    ](
        record: R,
        field: ForeignKeyFieldDescriptor[F, R, M],
        foreignMeta: MetaRecord[FR],
        fieldValue: F,
        obj: AnyRef
    ): Option[FR] = None

    override def mismatchedPrimaryKey[
        F,
        R <: Record[R],
        M <: MetaRecord[R],
        FR <: Record[FR] with HasPrimaryKey[F, FR]
    ](
        record: R,
        field: ForeignKeyFieldDescriptor[F, R, M],
        foreignMeta: MetaRecord[FR],
        fieldValue: F,
        foreignRecord: FR
    ): Option[FR] = None
  }

  var fkHooks: ForeignKeyHooks = NoopForeignKeyHooks
}

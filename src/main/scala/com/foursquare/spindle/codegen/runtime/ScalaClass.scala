// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.runtime

import com.twitter.thrift.descriptors.{Struct, StructProxy}

class ScalaClass(
    override val underlying: Struct,
    resolver: TypeReferenceResolver
) extends StructProxy with StructLike {
  private val primaryKeyFieldName: Option[String] = annotations.get("primary_key")
  private val foreignKeyFieldNames: Seq[String] = annotations.getAll("foreign_key")

  private val existingFieldNames: Set[String] = underlying.__fields.map(_.name).toSet
  for (pkField <- primaryKeyFieldName) {
    if (!existingFieldNames.contains(pkField)) {
      throw new CodegenException("Error: primary_key annotation references non-existing field %s".format(pkField))
    }
  }

  private val missingForeignKeyFieldNames: Set[String] = foreignKeyFieldNames.toSet -- existingFieldNames
  if (missingForeignKeyFieldNames.nonEmpty) {
    throw new CodegenException("Error: foreign_key annotations reference non-existing fields: %s".format(
      missingForeignKeyFieldNames.mkString(", ")))
  }

  override val __fields: Seq[ScalaField] =
    for (field <- underlying.__fields) yield {
      val isPrimaryKey = primaryKeyFieldName.exists(_ == field.name)
      val isForeignKey = foreignKeyFieldNames.exists(_ == field.name)
      new ScalaField(field, resolver, isPrimaryKey, isForeignKey)
    }

  override val primaryKeyField: Option[ScalaField] = __fields.find(_.isPrimaryKey)
}

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

  // Check that no retired_ids or retired_wire_names are being used in
  {
    // In an extra scope to prevent these vals from being public on the class
    val ids = __fields.map(_.identifier).toSet
    val wireNames = __fields.map(_.wireName).toSet
    val retiredIds = annotations.getAll("retired_ids").flatMap(_.split(',')).map(_.toShort).toSet
    val retiredWireNames = annotations.getAll("retired_wire_names").flatMap(_.split(',')).toSet
    val repeatedWireNames = __fields.groupBy(_.wireName).filter(_._2.size > 1).keys.toSeq
    val badIds = ids.intersect(retiredIds)
    val badWireNames = wireNames.intersect(retiredWireNames)
    if (repeatedWireNames.nonEmpty) {
      throw new CodegenException("Error: illegal repetition of wire_name's: %s".format(repeatedWireNames.mkString(", ")))
    }
    if (badIds.nonEmpty) {
      throw new CodegenException("Error: illegal use of retired ids: %s".format(badIds.mkString(", ")))
    }
    if (badWireNames.nonEmpty) {
      throw new CodegenException("Error: illegal use of retired wire names: %s".format(badWireNames.mkString(", ")))
    }
  }

  override val primaryKeyField: Option[ScalaField] = __fields.find(_.isPrimaryKey)
}

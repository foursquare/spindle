package com.foursquare.recordv2.codegen.runtime

object CodegenUtil {
  // List of Scala reserved words from Scala Language Specification (SLS) Section 1.1
  val ScalaReservedWords = Set(
    "abstract", "case", "class", "def", "do", "else", "extends", "false", "final", "finally", "for", "forSome",
    "if", "implicit", "import", "lazy", "match", "new", "null", "object", "override", "package", "private",
    "protected", "return", "sealed", "super", "this", "throw", "trait", "try", "true", "type", "val", "var", "while",
    "with", "yield")

  val RecordReservedWords = Set(
    // from Record.scala
    "meta",
    // from MetaRecord.scala
    "createRecord", "fields", "annotations", "recordName", "companionProvider",
    // from HasPrimaryKey.scala
    "primaryKey",
    // from class.mk
    "_Fields", "idToTFieldIdEnum", "apply", "Builder", "newBuilder", "copy", "mutableCopy", "validate",
    "getSetFields", "Id", "result", "resultMutable",
    // from Ordered.scala/Comparable.java
    "compare", "compareTo",
    // from TBase.java
    "write", "read", "fieldForId", "isSet", "getFieldValue", "setFieldValue", "deepCopy", "clear",
    // from Object.java
    "toString", "equals", "hashCode")

  val ReservedWords = ScalaReservedWords ++ RecordReservedWords

  def escapeScalaFieldName(name: String): String = {
    if (ReservedWords.contains(name)) "__" + name else name
  }
}

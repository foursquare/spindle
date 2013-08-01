package com.foursquare.spindle.codegen.runtime

trait StructLike extends HasAnnotations {
  def __fields: Seq[ScalaField]

  def name: String
  def fields: Seq[ScalaField] = __fields

  val tstructName = name.toUpperCase + "_DESC"
  def primaryKeyField: Option[ScalaField] = None
  def isException: Boolean = false
}

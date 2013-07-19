package com.foursquare.recordv2.codegen.runtime

/**
 * From package.scala:
 *   type EnhancedTypes = (TypeReference, Seq[Annotation], Scope) => Option[TypeReference]
 */
object EnhancedTypes {
  val Empty: EnhancedTypes = (ref, annots, scope) => Some(ref)
}

// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.runtime

/**
 * From package.scala:
 *   type EnhancedTypes = (TypeReference, Seq[Annotation], Scope) => Option[TypeReference]
 */
object EnhancedTypes {
  val Empty: EnhancedTypes = (ref, annots, scope) => Some(ref)
}

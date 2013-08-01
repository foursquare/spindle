package com.foursquare.spindle.codegen.runtime

import com.foursquare.spindle.Annotations

object `package` {
  type Scope = Map[String, TypeDeclaration]
  type EnhancedTypes = (TypeReference, Annotations, Scope) => Option[TypeReference]
}

package com.foursquare.recordv2.codegen.runtime

import com.foursquare.recordv2.Annotations

object `package` {
  type Scope = Map[String, TypeDeclaration]
  type EnhancedTypes = (TypeReference, Annotations, Scope) => Option[TypeReference]
}

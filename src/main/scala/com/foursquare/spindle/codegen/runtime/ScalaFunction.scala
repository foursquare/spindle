// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.runtime

import com.foursquare.spindle.Annotations
import com.twitter.thrift.descriptors.{Annotation, Field, Function, FunctionProxy, Requiredness}

class ScalaFunction(override val underlying: Function, resolver: TypeReferenceResolver) extends FunctionProxy with HasAnnotations {
  val returnRenderType: Option[RenderType] = (underlying
    .returnTypeIdOption
    .flatMap(typeId => resolver.typeForTypeId(typeId).right.toOption)
    .map(_._1)
    .flatMap(resolver.resolveType(_).right.toOption)
    .map(RenderType(_, Annotations.empty)))

  override val argz: Seq[ScalaField] = underlying.argz.map(new ScalaField(_, resolver))
  override val throwz: Seq[ScalaField] = underlying.throwz.map(new ScalaField(_, resolver))

  val successField: Option[ScalaField] = (underlying
    .returnTypeIdOption
    .map(typeId => {
      (Field
        .newBuilder
        .identifier(0.toShort)
        .name("success")
        .typeId(typeId)
        // NOTE(tdyas): Should this be Requiredness.REQUIRED? The Python codegen didn't do this, but needs a rethink.
        .requiredness(Requiredness.OPTIONAL)
        .__annotations(Seq(Annotation("builder_required", "true")))
        .result()
      )})
    .map(new ScalaField(_, resolver)))

  val fields: Seq[ScalaField] = successField.toSeq ++ this.throwz
}

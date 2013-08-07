// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.runtime

import com.twitter.thrift.descriptors.{Union, UnionProxy}

class ScalaUnion(override val underlying: Union, resolver: TypeReferenceResolver) extends UnionProxy with StructLike {
  override val __fields: Seq[ScalaField] = underlying.__fields.map(new ScalaField(_, resolver))
}

package com.foursquare.recordv2.codegen.runtime

import com.twitter.thrift.descriptors.{Service, ServiceProxy}

class ScalaService(override val underlying: Service, resolver: TypeReferenceResolver) extends ServiceProxy {
  override val functions: Seq[ScalaFunction] = underlying.functions.map(new ScalaFunction(_, resolver))
}

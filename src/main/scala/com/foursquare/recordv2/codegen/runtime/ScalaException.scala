package com.foursquare.recordv2.codegen.runtime

import com.twitter.thrift.descriptors.{Exception, ExceptionProxy}

class ScalaException(
    override val underlying: Exception,
    resolver: TypeReferenceResolver
) extends ExceptionProxy with StructLike {
  override val __fields: Seq[ScalaField] = underlying.__fields.map(new ScalaField(_, resolver))
  override def isException: Boolean = true
}

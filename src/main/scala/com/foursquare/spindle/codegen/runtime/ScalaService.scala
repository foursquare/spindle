// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.runtime

import com.twitter.thrift.descriptors.{Service, ServiceProxy}

class ScalaService(override val underlying: Service, resolver: TypeReferenceResolver) extends ServiceProxy with HasAnnotations {
  val parentServiceName: Option[String] = {
    extendzOption.flatMap(extendz => resolver.resolveTypeAlias(extendz) match {
      case Right(ServiceRef(name)) => Some(name)
      case _ => throw new CodegenException("In service %s, parent service %s is not defined."
        .format(nameOption.getOrElse(throw new IllegalStateException("service missing name")), extendz))
    })
  }

  override val functions: Seq[ScalaFunction] = underlying.functions.map(new ScalaFunction(_, resolver))
}

package com.foursquare.recordv2.codegen.runtime

import com.foursquare.recordv2.Annotations
import com.twitter.thrift.descriptors.Annotation

trait HasAnnotations {
  def __annotations: Seq[Annotation]

  val annotations: Annotations = makeAnnotations(__annotations)

  private def makeAnnotations(annotations: Seq[Annotation]): Annotations = {
    new Annotations(annotations.map(a => (a.key, a.value)))
  }
}

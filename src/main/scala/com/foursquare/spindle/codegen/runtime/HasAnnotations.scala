// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.runtime

import com.foursquare.spindle.Annotations
import com.twitter.thrift.descriptors.Annotation

trait HasAnnotations {
  def __annotations: Seq[Annotation]

  val annotations: Annotations = makeAnnotations(__annotations)

  private def makeAnnotations(annotations: Seq[Annotation]): Annotations = {
    new Annotations(annotations.map(a => (a.key, a.value)))
  }
}

// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle

trait FunctionDescriptor[RequestType <: Record[RequestType], ResponseType <: Record[ResponseType]] {
  /**
   * Returns the name of this function.
   */
  def functionName: String

  /**
   * Return the [[com.foursquare.spindle.MetaRecord]] for this method's arguments.
   */
  def requestMetaRecord: MetaRecord[RequestType]

  /**
   * Return the [[com.foursquare.spindle.MetaRecord]] for this method's response.
   */
  def responseMetaRecord: MetaRecord[ResponseType]
}

trait ServiceDescriptor {
  /**
   * Returns the name of this service.
   */
  def serviceName: String

  /**
   * Returns descriptors for the methods implemented by this service.
   * @return a sequence of [[com.foursquare.spindle.ServiceMethodDescriptor]]
   */
  def functionDescriptors: Seq[FunctionDescriptor[_,_]]
}
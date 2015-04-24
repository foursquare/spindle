package com.foursquare.common.thrift.base;


import org.apache.thrift.TException;

public class NonStringMapKeyException extends TException {
  public NonStringMapKeyException(Object key) {
    this(key, null);
  }

  public NonStringMapKeyException(Object key, Exception cause) {
    super(
      "Protocol requires string-typed map key, but got a " + key.getClass().getCanonicalName(),
      cause
    );
  }
}

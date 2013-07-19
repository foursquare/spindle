//  Copyright 2011 Foursquare Labs Inc. All Rights Reserved

package com.foursquare.common.thrift.bson;

import java.util.Date;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TSet;
import org.apache.thrift.protocol.TType;
import org.bson.BSONObject;
import org.bson.types.ObjectId;


/** Tracks state while converting a BSON object into a Thrift object.
 *
 * If any ClassCastExceptions occur this indicates either invalid BSON or a serious bug in the Thrift libraries or the
 * generated Thrift code (or in this code, of course). In these cases we throw a TException with details of the
 * unexpected context mismatch.
 */
public class BSONReadState {

  // The BSON context we're currently reading.
  private abstract static class ReadContext {
    // The current value we're reading.
    abstract Object getNextItem() throws TException;

    // Our best guess at the ttype of the current value. If the field is unknown, this will provide enough
    // information in order to skip the value correctly. If the field is known, we may override this with
    // more specific type information (e.g., we may know that an Integer actually represents an i16).
    abstract byte valueTType() throws TException;
  }

  // For reading key-value pairs out of a document.
  private static class DocumentReadContext extends ReadContext {
    // Map from final classes to corresponding TType. We can look these up quickly, without using instanceof,
    // because we know that if an object is assignable to this class then it must be of this class.
    private static Map<Class<?>, Byte> finalClassToTTypeMap = new HashMap<Class<?>, Byte>();
    static {
      finalClassToTTypeMap.put(Boolean.class, TType.BOOL);
      finalClassToTTypeMap.put(Integer.class, TType.I32);
      finalClassToTTypeMap.put(Long.class, TType.I64);
      finalClassToTTypeMap.put(Double.class, TType.DOUBLE);
      finalClassToTTypeMap.put(String.class, TType.STRING);
      finalClassToTTypeMap.put(byte[].class, TType.STRING);
    }

    // List of non-final classes a value might be of.
    private static class NonFinalClassToTTypeEntry {
      public NonFinalClassToTTypeEntry(Class<?> c, byte t) { clazz = c; ttype = t; }
      public final Class<?> clazz;
      public final byte ttype;
    }

    private static NonFinalClassToTTypeEntry[] nonFinalClassToTType = {
      // Note that order matters, since, e.g., BasicBSONList is a subtype of both List and BSONObject,
      // and we want to treat it as a List here.
      new NonFinalClassToTTypeEntry(Date.class, TType.I64),
      new NonFinalClassToTTypeEntry(List.class, TType.LIST),
      new NonFinalClassToTTypeEntry(ObjectId.class, TType.STRING),
      new NonFinalClassToTTypeEntry(BSONObject.class, TType.STRUCT)
    };

    // Note that the keySet is backed by the document's underlying map and will therefore be ordered.
    // Ordering is not required for correctness, but may be useful when testing.
    private Iterator<String> keyIter;
    private BSONObject document;
    private String currentKey = null;
    private Object currentValue = null;

    DocumentReadContext(BSONObject document) {
      this.document = document;
      keyIter = document.keySet().iterator();
    }

    Object getNextItem() throws TException {
      if (currentKey == null) {  // The next item (if any) is a document key.
        currentValue = null;
        // skip items with null values
        while (keyIter.hasNext() && currentValue == null) {
          currentKey = keyIter.next();
          currentValue = document.get(currentKey);
        }
        // don't return a key if the value is null.
        if (currentValue == null) {
          currentKey = null;
        }
        return currentKey;
      } else {  // The next item is a document value.
        currentKey = null;
        return currentValue;  // Guaranteed not to be null.
      }
    }

    byte valueTType() throws TException {
      Class<?> classOfCurrentValue = currentValue.getClass();
      Byte ret = finalClassToTTypeMap.get(classOfCurrentValue);
      if (ret == null) {
        for (int i = 0; i < nonFinalClassToTType.length; ++i) {
          if (nonFinalClassToTType[i].clazz.isAssignableFrom(classOfCurrentValue)) {
            ret = nonFinalClassToTType[i].ttype;
            break;
          }
        }
      }
      if (ret == null) {
        throw new TException("Unknown TType for value of class " + classOfCurrentValue.getName());
      }
      return ret;
    }
  }

  // For reading values out of an array.
  private static class ArrayReadContext extends ReadContext {
    private Iterator<Object> iter;

    ArrayReadContext(List<Object> array) {
      iter = array.iterator();
    }

    Object getNextItem() throws TException {
      // A BSON array may contain gaps, so we iterate past them.
      Object ret = null;
      while (ret == null && iter.hasNext()) { ret = iter.next(); }
      if (ret == null && !iter.hasNext()) { throw new TException("Attempted to read past end of array"); }
      return ret;  // Guaranteed not to be null.
    }

    byte valueTType() throws TException {
      throw new TException("valueTType() should never be called in array context");
    }
  }

  // A sentinel to indicate that there are no more fields to read.
  private static final TField NO_MORE_FIELDS = new TField("", TType.STOP, (short)0);

  // When use this when don't know the type of a field or a collection value. The read() method doesn't need
  // this information from us anyway, since it already knows the type of the field/collection it's reading,
  // and we can skip unknown fields without this information.
  private static final byte UNKNOWN_TTYPE = TType.VOID;

  // Negative field ids are not allowed, so this will never be recognized by the generated code.
  private static final short UNKNOWN_FIELD_ID = (short)-1;

  // The top-level object we read from.
  private BSONObject srcObject;

  // The stack of nested BSON read contexts. Each context represents either a document or an array.
  private Stack<ReadContext> readContextStack = new Stack<ReadContext>();

  // Set the top-level object we're reading from.
  void setSource(BSONObject srcObj) throws TException {
    if (this.srcObject != null || !readContextStack.empty()) {
      throw new TException("Can't set the source object in the middle of reading");
    }
    this.srcObject = srcObj;
  }

  ReadContext currentReadContext() throws TException {
    try {
      return readContextStack.peek();
    } catch (EmptyStackException e) {
      throw new TException("Can't read outside a document or array context");
    }
  }

  void readStructBegin() throws TException {
    if (srcObject == null) {
      throw new TException("You must call setSource() on the TBSONObjectProtocol instance in order to read from it.");
    }
    try {
      BSONObject obj = readContextStack.empty() ? srcObject : (BSONObject)getNextItem();
      readContextStack.push(new DocumentReadContext(obj));
    } catch (ClassCastException e) {
      throw new TException("Expected struct value");
    }
  }

  TMap readMapBegin() throws TException {
    try {
      BSONObject obj = (BSONObject)getNextItem();
      readContextStack.push(new DocumentReadContext(obj));
      int size = obj.keySet().size();
      for (String key : obj.keySet()) { if (obj.get(key) == null) size--; }
      return new TMap(UNKNOWN_TTYPE, UNKNOWN_TTYPE, size);
    } catch (ClassCastException e) {
      throw new TException("Expected map value");
    }
  }

  @SuppressWarnings("unchecked")
  TList readListBegin() throws TException {
    try {
      List<Object> list = (List<Object>)getNextItem();
      int size = list.size();
      for (Object x : list) { if (x == null) size--; }
      readContextStack.push(new ArrayReadContext(list));
      return new TList(UNKNOWN_TTYPE, size);
    } catch (ClassCastException e) {
      throw new TException("Expected list value");
    }
  }

  @SuppressWarnings("unchecked")
  TSet readSetBegin() throws TException {
    try {
      List<Object> list = (List<Object>)getNextItem();  // BSON has no set type, so we assume a list instead.
      int size = list.size();
      for (Object x : list) { if (x == null) size--; }
      readContextStack.push(new ArrayReadContext(list));
      return new TSet(UNKNOWN_TTYPE, size);
    } catch (ClassCastException e) {
      throw new TException("Expected set value");
    }
  }

  ReadContext readEnd() throws TException {
    try {
      return readContextStack.pop();
    } catch (EmptyStackException e) {
      throw new TException("Can't pop off an empty read context stack");
    }
  }

  TField nextField() throws TException {
    Object item = currentReadContext().getNextItem();
    if (item == null) {
      // We've exhausted all the fields in the current document.
      return NO_MORE_FIELDS;
    }
    try {
      // The next field name is a BSON document key, which must be a string.
      String name = (String)item;
      // We don't know the id, let the caller figure it out from the name.
      return new TField(name, currentReadContext().valueTType(), UNKNOWN_FIELD_ID);
    } catch (ClassCastException e) {
      throw new TException("Expected string document key but got a " + item.getClass().getName());
    }
  }

  Object getNextItem() throws TException {
    return currentReadContext().getNextItem();
  }

  void reset() {
    srcObject = null;
    readContextStack.clear();
  }

  boolean inFlight() {
    return !readContextStack.isEmpty();
  }
}

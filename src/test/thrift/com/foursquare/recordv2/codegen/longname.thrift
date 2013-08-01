namespace java com.foursquare.spindle.test.gen

enum LongNameTestEnum {
  Zero = 0
  One = 1
  Two = 2
}

struct LongNameInnerStruct {
  1: optional string aString (wire_name="n")
  2: optional i32 anInt (wire_name="o")
}

struct LongNameTestStruct {
  1: optional bool aBool (wire_name="a")
  2: optional byte aByte (wire_name="b")
  3: optional i16 anI16 (wire_name="c")
  4: optional i32 anI32 (wire_name="d")
  5: optional i64 anI64 (wire_name="e")
  6: optional double aDouble (wire_name="f")
  7: optional string aString (wire_name="g")
  8: optional binary aBinary (wire_name="h")
  9: optional LongNameInnerStruct aStruct (wire_name="i")
  10: optional set<string> aSet (wire_name="j")
  11: optional list<i32> aList (wire_name="k")
  12: optional map<string, LongNameInnerStruct> aMap (wire_name="l")
  13: optional LongNameTestEnum anEnum (wire_name="m")
}

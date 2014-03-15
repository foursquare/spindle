// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

namespace java com.foursquare.spindle.test.gen

enum OldTestEnum {
  Zero = 0
  One = 1
}

enum NewTestEnum {
  Zero = 0
  One = 1
  Two = 2
}

struct StructWithOldEnumField {
  1: optional OldTestEnum anEnum
  2: optional list<OldTestEnum> anEnumList
}

struct StructWithNewEnumField {
  1: optional NewTestEnum anEnum
  2: optional list<NewTestEnum> anEnumList
}
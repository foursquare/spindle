// Copyright 2014 Foursquare Labs Inc. All Rights Reserved.

namespace java com.foursquare.spindle.test.gen

typedef binary (enhanced_types="bson:ObjectId") ObjectId

struct MapsWithNonStringKeys {
  1: optional map<i32, string> foo
  2: optional map<ObjectId, i32> bar
}

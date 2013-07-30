namespace java com.foursquare.thriftexample.av


include "com/foursquare/thriftexample/talent/actor.thrift"
include "com/foursquare/thriftexample/talent/crewmember.thrift"


typedef crewmember.CrewMember CrewMember


struct Movie {
  5: optional i32 id (wire_name="id", builder_required="true")
  1: required string name (wire_name="name", builder_required="true")
  2: required i32 lengthMinutes
  3: optional map<string, actor.Actor> cast  // E.g., "Austin Powers" -> Mike Myers
  4: optional list<CrewMember> crew
} (
  primary_key="id"

  index="id: asc"
  index="name: asc, lengthMinutes: desc"
  mongo_collection="movies"
  mongo_identifier="fake"
)

exception MovieException {
  1: optional list<string> problems
}

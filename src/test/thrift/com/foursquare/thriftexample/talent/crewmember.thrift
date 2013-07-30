namespace java com.foursquare.thriftexample.talent

include "com/foursquare/thriftexample/people/person.thrift"


struct CrewMember {
  1: required person.Person details
  2: required list<string> credits  // E.g., "2nd Assistant Director"
}

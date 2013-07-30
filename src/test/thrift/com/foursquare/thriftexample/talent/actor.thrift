namespace java com.foursquare.thriftexample.talent

include "com/foursquare/thriftexample/people/person.thrift"

typedef person.Person PersonDetails

struct Actor {
  1: required PersonDetails details
  2: optional PersonDetails agentDetails
}

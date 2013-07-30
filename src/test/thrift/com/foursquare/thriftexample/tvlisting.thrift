namespace java com.foursquare.thriftexample

include "com/foursquare/thriftexample/av/tv.thrift"
include "com/foursquare/thriftexample/av/movie.thrift"

typedef string DateTime // String in the format YYYY-MM-DD HH:MM:SS

union Content {
  1: optional tv.TvShowEpisode show
  2: optional movie.Movie movie
}

struct TvListingEntry {
  1: DateTime startTime (wire_name="st")
  2: DateTime endTime (wire_name="et")
  3: Content content
}

typedef list<TvListingEntry> TvListing



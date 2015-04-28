# Copyright 2015 Foursquare Labs Inc. All Rights Reserved.

def sjar(org, name, rev, url=None, intransitive=False):
  scala_jar_version = '2.10'
  return jar(org = org, name = '%s_%s' % (name, scala_jar_version), rev = rev, url = url, intransitive = intransitive)

jar_library(
  name = 'joda-time',
  jars = [
    jar(org = 'joda-time', name = 'joda-time', rev = '2.2_2013h'),
    jar(org = 'org.joda', name = 'joda-convert', rev = '1.2'),
  ],
)

jar_library(
  name = 'rogue-field',
  jars = [
    sjar(org = 'com.foursquare', name = 'rogue-field', rev = '2.2.1', intransitive = True),
  ],
)

jar_library(
  name = 'thrift',
  jars = [
    jar(org = 'org.apache.thrift', name = 'libthrift', rev = '0.9.0')
      .exclude(org='org.apache.httpcomponents', name='httpclient'),  # Depends on a bad version.
  ],
  dependencies = [
    ':apache-httpclient'
  ],
)

jar_library(
  name = 'apache-httpclient',
  jars = [
    jar(org = 'org.apache.httpcomponents', name = 'httpclient', rev = '4.3.5'),
  ],
)

jar_library(
  name = 'mongodb',
  jars = [jar(org = 'org.mongodb', name = 'mongo-java-driver', rev = '2.11.3_fs-c')],
)

jar_library(
  name = 'jackson',
  jars = [
    jar(org = 'org.codehaus.jackson', name = 'jackson-core-asl', rev = '1.9.8'),
    jar(org = 'org.codehaus.jackson', name = 'jackson-mapper-asl', rev = '1.9.8'),
    jar(org = 'org.codehaus.jackson', name = 'jackson-xc', rev = '1.9.8'),
  ],
)

jar_library(
  name = 'scalaj-collection',
  jars = [sjar(org = 'org.scalaj', name = 'scalaj-collection', rev = '1.5')],
)

jar_library(
  name = 'commons-io',
  jars = [jar(org = 'commons-io', name = 'commons-io', rev = '2.1')],
)

jar_library(
  name = 'parboiled-scala',
  jars = [jar(org = 'org.parboiled', name = 'parboiled-scala_2.10', rev = '1.1.4')],
)

jar_library(
  name = 'junit',
  jars = [
    jar(org = 'junit', name = 'junit', rev = '4.11'),
    jar(org = 'com.novocode', name = 'junit-interface', rev = '0.7'),
  ],
)

jar_library(
  name = 'scalate',
  jars = [jar(org = 'org.fusesource.scalate', name = 'scalate-core_2.10', rev = '1.6.1')],
)

jar_library(
  name = 'argot',
  jars = [sjar(org = 'org.clapper', name = 'argot', rev = '1.0.1')],
)

jar_library(
  name = 'twitter-util',
  jars = [sjar(org = 'com.twitter', name = 'util-core', rev = '6.16.0')],
)

finagle_rev = '6.16.0'
jar_library(name = 'finagle',
  jars = [
    jar(org = 'com.twitter', name = 'finagle-core_2.10', rev = finagle_rev),
    jar(org = 'com.twitter', name = 'finagle-http_2.10', rev = finagle_rev),
    jar(org = 'com.twitter', name = 'finagle-stream_2.10', rev = finagle_rev),
    jar(org = 'com.twitter', name = 'finagle-kestrel_2.10', rev = finagle_rev),
    jar(org = 'com.twitter', name = 'finagle-ostrich4_2.10', rev = finagle_rev),
    jar(org = 'com.twitter', name = 'finagle-serversets_2.10', rev = finagle_rev),
    jar(org = 'com.twitter', name = 'finagle-thrift_2.10', rev = finagle_rev),
    jar(org = 'com.twitter', name = 'finagle-memcached_2.10', rev = finagle_rev),
  ],
)

jar_library(name = 'specs',
  jars = [
    sjar(org = 'org.scala-tools.testing', name = 'specs', rev = '1.6.11-fs'),
  ],
  dependencies = [
    ':mockito'
  ],
)

jar_library(
  name = 'mockito',
  jars = [jar(org = 'org.mockito', name = 'mockito-all', rev = '1.9.0')],
)

jar_library(
  name = 'phonenumbers',
  jars = [jar(org = 'com.googlecode.libphonenumber', name = 'libphonenumber', rev = '6.2.2')],
)

jar_library(
  name = 'scala-compiler',
  jars = [
    jar(org = 'org.scala-lang', name = 'scala-compiler', rev = '2.10.4', force=True),
    jar(org = 'org.scala-lang', name = 'scala-library', rev = '2.10.4', force=True),
  ],
)
